package comp0012.main;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Select;

public class InstructionGraph {

	private final MethodGen mg;
	private final Map<InstructionHandle, Set<InstructionHandle>> successors;

	public InstructionGraph(MethodGen mg) {
		this.mg = mg;
		successors = new HashMap<>();
		
		build();
	}
	
	public void remove(InstructionHandle ih) {
		successors.remove(ih);
	}
	
	public Set<InstructionHandle> getSuccessors(InstructionHandle ih) {
		return new HashSet<>(successors.get(ih));
	}
	
	public Set<InstructionHandle> getPredecessors(InstructionHandle ih) {
		Set<InstructionHandle> preds = new HashSet<>();
		for(Entry<InstructionHandle, Set<InstructionHandle>> e : successors.entrySet()) {
			if(e.getValue().contains(ih)) {
				preds.add(e.getKey());
			}
		}
		return preds;
	}
	
	private Set<InstructionHandle> findSuccessors(InstructionHandle ih) {
		Instruction i = ih.getInstruction();
		if(i instanceof ReturnInstruction || i instanceof ATHROW) {
			return Collections.emptySet();
		} else if(i instanceof GotoInstruction) {
			return Collections.singleton(((GotoInstruction) i).getTarget());
		} else if(i instanceof BranchInstruction) {
			Set<InstructionHandle> succs = new HashSet<>();
			succs.add(((BranchInstruction) i).getTarget());
			succs.add(ih.getNext());
			if(i instanceof Select) {
				succs.addAll(Arrays.asList(((Select) i).getTargets()));
			}
			return succs;
		} else {
			return Collections.singleton(ih.getNext());
		}
	}

	private void build() {
		for (InstructionHandle ih = mg.getInstructionList().getStart(); ih != null; ih = ih.getNext()) {
			successors.put(ih, findSuccessors(ih));
		}
	}
	
	public String toDotGraph() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph {\n");
		for(InstructionHandle ih : successors.keySet()) {
			sb.append("  ").append(ih.getPosition()).append(" [label=\"").append(ih.toString()).append("\"];").append("\n");
		}
		for(Entry<InstructionHandle, Set<InstructionHandle>> e : successors.entrySet()) {
			for(InstructionHandle s : e.getValue()) {
				sb.append("  ").append(e.getKey().getPosition()).append(" -> ").append(s.getPosition()).append(";\n");
			}
		}
		return sb.append("}").toString();
	}
}
