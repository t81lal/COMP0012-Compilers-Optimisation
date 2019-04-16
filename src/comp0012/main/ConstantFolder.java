package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import comp0012.main.Value.ConstantValue;

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
		for (Method m : gen.getMethods()) {
			if(!(gen.getClassName() + "." + m.getName()).equals("comp0012.target.ConstantVariableFolding.methodOne"))
				continue;
			System.out.println(gen.getClassName() + "." + m.getName());
			MethodGen mg = new MethodGen(m, gen.getClassName(), cpgen);
			optimise(mg);
		}
		optimized = gen.getJavaClass();
	}

	private void optimise(MethodGen mg) {
		FrameAnalysis fa = new FrameAnalysis(mg);
		fa.solve();
		ConstantPoolGen cpg = mg.getConstantPool();
		
		InstructionList list = mg.getInstructionList();
		for (InstructionHandle ih = list.getStart(); ih != null; ih = ih.getNext()) {
			Instruction i = ih.getInstruction();
			if(i instanceof LoadInstruction) {
				LoadInstruction li = (LoadInstruction) i;
				Value val = fa.getFact(ih).getLocal(li.getIndex());
				if(val instanceof ConstantValue) {
					Object cst = ((ConstantValue) val).getConstant();
					if(cst instanceof Number) {
						
					}
				}
			}
		}
	}
	
	private int setInstruction(ConstantPoolGen cpg, InstructionHandle ih, Number n) {
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
			return 0;
		}
		return 1;
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