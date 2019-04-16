package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import comp0012.main.Value.ConstantValue;
import comp0012.main.Value.ProducedValue;

public class ConstantFolder {
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath) {
		try {
			parser = new ClassParser(classFilePath);
			original = parser.parse();
			gen = new ClassGen(original);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void optimize() {
		ConstantPoolGen cpgen = gen.getConstantPool();
		Method[] methods = gen.getMethods();
		for (int i=0; i < methods.length; i++) {
			Method m = methods[i];
			if(!(gen.getClassName() + "." + m.getName()).equals("comp0012.target.ConstantVariableFolding.methodOne"))
				continue;
			System.out.println(gen.getClassName() + "." + m.getName());
			MethodGen mg = new MethodGen(m, gen.getClassName(), cpgen);
			boolean change;
			do {
				change = false;
				change = optimise(mg);
			} while(change);
			gen.setMethodAt(mg.getMethod(), i);
		}
		optimized = gen.getJavaClass();
	}

	private boolean optimise(MethodGen mg) {
		boolean change = false;
		change |= propagateConstants(mg);
		change |= removeDeadAssignments(mg);
		return change;
	}
	
	private boolean removeDeadAssignments(MethodGen mg) {
		LivenessAnalysis la = new LivenessAnalysis(mg);
		la.solve();
		
		FrameAnalysis fa = new FrameAnalysis(mg);
		fa.solve();

		boolean change = false;
		InstructionList list = mg.getInstructionList();
		for (InstructionHandle ih = list.getStart(); ih != null; ih = ih.getNext()) {
			Instruction i = ih.getInstruction();
			if(i instanceof StoreInstruction) {
				StoreInstruction si = (StoreInstruction) i;
				Set<Integer> liveOut = la.getOutFact(ih);
				/* assignment is unused, remove it if we can */
				if(!liveOut.contains(si.getIndex())) {
					/* The top element on the stack of the pre-execution frame
					 * contains the value that will be stored when this instruction is executed.
					 * For now, we can remove it if it's a constant (TODO: simple side effect decisions). */
					Frame inFrame = fa.getInFact(ih);
					ProducedValue pv = (ProducedValue) inFrame.peek();
					Value v = pv.getCoreValue();
					if(v instanceof ConstantValue) {
						Set<InstructionHandle> cstProducers = pv.getProducers();
						for(InstructionHandle p : cstProducers) {
							p.setInstruction(new NOP());
						}
						ih.setInstruction(new NOP());
						change = true;
					}
				}
			}
		}
		
		return change;
	}
	
	private boolean propagateConstants(MethodGen mg) {
		FrameAnalysis fa = new FrameAnalysis(mg);
		fa.solve();
		
		ConstantPoolGen cpg = mg.getConstantPool();
		InstructionList list = mg.getInstructionList();
		boolean change = false;
		
		for (InstructionHandle ih = list.getStart(); ih != null; ih = ih.getNext()) {
			Instruction i = ih.getInstruction();
			if(i instanceof LoadInstruction) {
				LoadInstruction li = (LoadInstruction) i;
				ProducedValue pv = (ProducedValue) fa.getInFact(ih).getLocal(li.getIndex());
				Value val = pv.getCoreValue();
				if(val instanceof ConstantValue) {
					Object cst = ((ConstantValue) val).getConstant();
					if(cst instanceof Number) {
						change |= setInstruction(cpg, ih, (Number) cst);
					}
				}
			}
		}
		return change;
	}
	
	private boolean setInstruction(ConstantPoolGen cpg, InstructionHandle ih, Number n) {
		Instruction i;
		if(n instanceof Float) {
			i = new LDC(cpg.addFloat(n.floatValue()));
		} else if(n instanceof Integer) {
			i = new LDC(cpg.addInteger(n.intValue()));
		} else if(n instanceof Double) {
			i = new LDC2_W(cpg.addDouble(n.doubleValue()));
		} else if(n instanceof Long) {
			i = new LDC2_W(cpg.addLong(n.longValue()));
		} else {
			return false;
		}
		ih.setInstruction(i);
		return true;
	}

	public void write(String optimisedFilePath) {
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(
					new File(optimisedFilePath));
			optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}