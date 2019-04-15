package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;

public class ConstantFolder {
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath) {
		try {
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
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
		this.optimized = gen.getJavaClass();
	}

	private void optimise(MethodGen mg) {
		DataFlowAnalysis<String> df = new DataFlowAnalysis<String>(mg) {
			
			@Override
			protected String transfer(InstructionHandle ins, String curFact) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			protected String merge(String in1, String in2) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			protected Map<InstructionHandle, String> getInputFacts() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public void write(String optimisedFilePath) {
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(
					new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}