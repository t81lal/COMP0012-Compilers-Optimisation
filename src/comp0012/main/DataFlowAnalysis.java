package comp0012.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.verifier.structurals.ControlFlowGraph;
import org.apache.bcel.verifier.structurals.ExecutionVisitor;
import org.apache.bcel.verifier.structurals.Frame;
import org.apache.bcel.verifier.structurals.InstConstraintVisitor;
import org.apache.bcel.verifier.structurals.InstructionContext;
import org.apache.bcel.verifier.structurals.UninitializedObjectType;

public abstract class DataFlowAnalysis<F> {

	private static class ExecutionState {
		final InstructionContext ins;
		final ArrayList<InstructionContext> path;
		public ExecutionState(InstructionContext ins,
				ArrayList<InstructionContext> path) {
			this.ins = ins;
			this.path = path;
		}
		
		public ExecutionState(InstructionContext ins) {
			this(ins, new ArrayList<>());
		}
	}

	private final MethodGen mg;
	private final ControlFlowGraph cfg;
	private Map<InstructionHandle, F> inFacts, outFacts;

	@SuppressWarnings("unchecked")
	private void solve() {
		/* Don't handle exceptional or subroutine control flow */
		List<ExecutionState> states = new ArrayList<>();
		InstructionContext ins0 = getInitialContext();
		Frame frame0 = getInitialFrame();
		ExecutionState state0 = new ExecutionState(ins0);

		InstConstraintVisitor icv = new InstConstraintVisitor();
		icv.setConstantPoolGen(mg.getConstantPool());
		icv.setMethodGen(mg);
		ExecutionVisitor ev = new ExecutionVisitor();
		ev.setConstantPoolGen(mg.getConstantPool());
		states.add(state0);
		
		ins0.execute(frame0, state0.path, icv, ev);
		
		while(!states.isEmpty()) {
			ExecutionState s = states.remove(0);
			
			ArrayList<InstructionContext> oldPath = (ArrayList<InstructionContext>) s.path.clone();
			ArrayList<InstructionContext> newPath = (ArrayList<InstructionContext>) s.path.clone();
			newPath.add(s.ins);
			
			for(InstructionContext succ : s.ins.getSuccessors()) {
				if(succ.execute(s.ins.getOutFrame(oldPath), newPath, icv, ev)) {
					states.add(new ExecutionState(succ, newPath));
				}
			}
		}
		
		InstructionHandle ih = ins0.getInstruction();
		while(ih != null) {
			InstructionContext ic = cfg.contextOf(ih);
			System.out.println(ih);
			System.out.println(ic.getInFrame().getStack());
			System.out.println("=======================");
			ih = ih.getNext();
		}
	}

	private InstructionContext getInitialContext() {
		return cfg.contextOf(mg.getInstructionList().getStart());
	}

	private Frame getInitialFrame() {
		Frame frame0 = new Frame(mg.getMaxLocals(), mg.getMaxStack());
		if (!mg.isStatic()) {
			if (mg.getName().equals("<init>")) {
				frame0.getLocals().set(0, new UninitializedObjectType(
						new ObjectType(mg.getClassName())));
			} else {
				frame0.getLocals().set(0, new ObjectType(mg.getClassName()));
			}
		}

		final Type[] argtypes = mg.getArgumentTypes();
		int twoslotoffset = 0;
		for (int j = 0; j < argtypes.length; j++) {
			if (argtypes[j] == Type.SHORT || argtypes[j] == Type.BYTE
					|| argtypes[j] == Type.CHAR
					|| argtypes[j] == Type.BOOLEAN) {
				argtypes[j] = Type.INT;
			}
			frame0.getLocals().set(twoslotoffset + j + (mg.isStatic() ? 0 : 1),
					argtypes[j]);
			if (argtypes[j].getSize() == 2) {
				twoslotoffset++;
				frame0.getLocals().set(
						twoslotoffset + j + (mg.isStatic() ? 0 : 1),
						Type.UNKNOWN);
			}
		}
		return frame0;
	}
	public DataFlowAnalysis(MethodGen mg) {
		this.mg = mg;
		cfg = new ControlFlowGraph(mg);
		inFacts = new HashMap<>();
		outFacts = new HashMap<>();
		solve();
	}


	protected abstract Map<InstructionHandle, F> getInputFacts();

	protected abstract F transfer(InstructionHandle ins, F curFact);

	protected abstract F merge(F in1, F in2);
}
