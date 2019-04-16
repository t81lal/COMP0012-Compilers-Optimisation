package comp0012.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import comp0012.main.ReachingDefinitionsAnalysis.Assign;

public class ReachingDefinitionsAnalysis extends DFA<Map<Integer, Set<Assign>>> {
	
	static abstract class Assign {
		final int index;
		Assign(int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return index;
		}
	}
	
	static class ParameterAssign extends Assign {
		ParameterAssign(int index) {
			super(index);
		}
		
		@Override
		public int hashCode() {
			return index * 13;
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof ParameterAssign && ((ParameterAssign) o).index == index;
		}
		
		@Override
		public String toString() {
			return "param@" + index;
		}
	}
	
	static class LocalAssign extends Assign {
		InstructionHandle ih;
		LocalAssign(int index, InstructionHandle ih) {
			super(index);
			this.ih = ih;
		}
		
		public InstructionHandle getHandle() {
			return ih;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(index, ih);
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof LocalAssign) {
				LocalAssign la = (LocalAssign) o;
				return la.index == index && Objects.equals(ih, la.ih);
			} else {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return "var" + index + "@" + ih.getPosition();
		}
	}
	
	private final Set<Integer> defs;
	
	public ReachingDefinitionsAnalysis(MethodGen mg) {
		super(mg, false);
		defs = getAllDefinitions();
	}
	
	private Set<Integer> getAllDefinitions() {
		/* Quick and dirty way to get all the definable locals (but not necessarily the defined locals). */
		Set<Integer> set = new HashSet<>();
		for (int i = 0; i < mg.getMaxLocals(); i++) {
			set.add(i);
		}
		return set;
	}

	@Override
	protected Map<InstructionHandle, Map<Integer, Set<Assign>>> getInputFacts() {
		Map<InstructionHandle, Map<Integer, Set<Assign>>> res = new HashMap<>();
		
		/* The parameters are considered reaching on entry to the method (as well as var0 for
		 * virtual methods). */
		Map<Integer, Set<Assign>> f = emptyFact();
		int idx = 0;
		if (!mg.isStatic()) {
			f.get(idx).add(new ParameterAssign(idx));
			idx++;
		}
		Type[] args = mg.getArgumentTypes();
		for (int i = 0; i < args.length; i++) {
			f.get(idx).add(new ParameterAssign(idx));
			idx += args[i].getSize();
		}
		res.put(mg.getInstructionList().getStart(), f);
		
		return res;
	}

	@Override
	protected Map<Integer, Set<Assign>> transfer(InstructionHandle ins, Map<Integer, Set<Assign>> curFact) {
		Map<Integer, Set<Assign>> out = copyFact(curFact);
		Instruction i = ins.getInstruction();
		if(i instanceof StoreInstruction) {
			StoreInstruction si = (StoreInstruction) i;
			/* This assignment kills all previous definitions for the same local and 'overwrites'/shadows them. */
			out.get(si.getIndex()).clear();
			out.get(si.getIndex()).add(new LocalAssign(si.getIndex(), ins));
		}
		return out;
	}

	@Override
	protected Map<Integer, Set<Assign>> emptyFact() {
		/* Empty state: no assignments, but we still 'fill out' the map so that map.get(d) for all d in defs
		 * doesn't return null. */
		Map<Integer, Set<Assign>> f = new HashMap<>();
		for(int i : defs) {
			f.put(i, new HashSet<>());
		}
		return f;
	}

	@Override
	protected Map<Integer, Set<Assign>> copyFact(Map<Integer, Set<Assign>> f) {
		/* We can't use new HashMap(f) because the new Map will have references to the old Map's Sets and we
		 * require a deep copy here. */
		Map<Integer, Set<Assign>> f2 = emptyFact();
		for (int i : defs) {
			f2.get(i).addAll(f.get(i));
		}
		return f2;
	}

	@Override
	protected Map<Integer, Set<Assign>> merge(Map<Integer, Set<Assign>> in1, Map<Integer, Set<Assign>> in2) {
		/* Merge operator: set union */
		Map<Integer, Set<Assign>> f = emptyFact();
		for(int i : defs) {
			f.get(i).addAll(in1.get(i));
			f.get(i).addAll(in2.get(i));
		}
		return f;
	}
}
