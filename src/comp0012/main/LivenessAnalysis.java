package comp0012.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.StoreInstruction;

public class LivenessAnalysis extends DFA<Set<Integer>> {

	public LivenessAnalysis(MethodGen mg) {
		super(mg, true);
	}

	@Override
	protected Map<InstructionHandle, Set<Integer>> getInputFacts() {
		Map<InstructionHandle, Set<Integer>> res = new HashMap<>();
		for (InstructionHandle ih = mg.getInstructionList().getStart(); ih != null; ih = ih.getNext()) {
			Instruction i = ih.getInstruction();
			if (i instanceof ReturnInstruction || i instanceof ATHROW) {
				res.put(ih, new HashSet<Integer>());
			}
		}
		return res;
	}

	@Override
	protected Set<Integer> transfer(InstructionHandle ih, Set<Integer> curFact) {
		Set<Integer> out = new HashSet<>(curFact);
		Instruction i = ih.getInstruction();
		if (i instanceof StoreInstruction) {
			StoreInstruction si = (StoreInstruction) i;
			out.remove(si.getIndex());
		}
		if (i instanceof LoadInstruction) {
			LoadInstruction li = (LoadInstruction) i;
			out.add(li.getIndex());
		}
		return out;
	}

	@Override
	protected Set<Integer> emptyFact() {
		return new HashSet<>();
	}

	@Override
	protected Set<Integer> copyFact(Set<Integer> f) {
		return new HashSet<>(f);
	}

	@Override
	protected Set<Integer> merge(Set<Integer> in1, Set<Integer> in2) {
		Set<Integer> out = new HashSet<>();
		out.addAll(in1);
		out.addAll(in2);
		return out;
	}
}
