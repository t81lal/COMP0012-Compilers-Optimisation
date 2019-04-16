package comp0012.main;

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

public class FrameAnalysis extends DFA<Frame> {

	public FrameAnalysis(MethodGen mg) {
		super(mg, false);
	}

	@Override
	protected Map<InstructionHandle, Frame> getInputFacts() {
		Map<InstructionHandle, Frame> res = new HashMap<>();
		res.put(mg.getInstructionList().getStart(), Frame.makeFrame(mg));
		return res;
	}

	@Override
	protected Frame transfer(InstructionHandle ins, Frame curFact) {
		return StaticInterpreter.execute(curFact, mg.getConstantPool(), ins);
	}

	@Override
	protected Frame emptyFact() {
		return new Frame(mg.getMaxLocals());
	}

	@Override
	protected Frame copyFact(Frame f) {
		return new Frame(f);
	}

	@Override
	protected Frame merge(Frame in1, Frame in2) {
		return Frame.merge(in1, in2);
	}
}
