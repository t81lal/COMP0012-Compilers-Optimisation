package comp0012.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

public abstract class DFA<F> {

	protected final MethodGen mg;
	protected final InstructionGraph graph;
	private final boolean backwards;
	private final Map<InstructionHandle, F> inFacts, outFacts;

	public DFA(MethodGen mg, boolean backwards) {
		this.mg = mg;
		this.backwards = backwards;
		graph = new InstructionGraph(mg);
		inFacts = new HashMap<>();
		outFacts = new HashMap<>();
	}

	public F getInFact(InstructionHandle ih) {
		return backwards ? outFacts.get(ih) : inFacts.get(ih);
	}
	
	public F getOutFact(InstructionHandle ih) {
		return backwards ? inFacts.get(ih) : outFacts.get(ih);
	}

	public void solve() {
		LinkedList<InstructionHandle> wl = new LinkedList<>();
		for (Entry<InstructionHandle, F> e : getInputFacts().entrySet()) {
			inFacts.put(e.getKey(), e.getValue());
			wl.add(e.getKey());
		}

		while (!wl.isEmpty()) {
			InstructionHandle ih = wl.pop();
			F in = merge(ih);
			inFacts.put(ih, in);
			F newOut = transfer(ih, in);
			if (!outFacts.containsKey(ih) || !outFacts.get(ih).equals(newOut)) {
//				if(outFacts.containsKey(ih) && !outFacts.get(ih).equals(newOut)) {
//					System.out.println("==========+=========");
//					System.out.print(outFacts.get(ih));
//					System.out.println("===================");
//					System.out.println(newOut);
//					System.out.println("===================");
//				}
				outFacts.put(ih, newOut);
				for (InstructionHandle succ : backwards ? graph.getPredecessors(ih) : graph.getSuccessors(ih)) {
					if (!wl.contains(succ)) {
						wl.addLast(succ);
					}
				}
			}
		}
	}

	private F merge(InstructionHandle ih) {
		Set<InstructionHandle> preds = backwards ? graph.getSuccessors(ih) : graph.getPredecessors(ih);
		if (preds.size() == 0) {
			return copyFact(inFacts.get(ih));
		} else if (preds.size() == 1) {
			InstructionHandle pred = preds.iterator().next();
			F predOut = outFacts.get(pred);
			F x = copyFact(predOut);
			return x;
		} else {
			Iterator<InstructionHandle> predIt = preds.iterator();
			Set<F> fs = new HashSet<>();
			while(predIt.hasNext()) {
				InstructionHandle p = predIt.next();
				if(outFacts.containsKey(p)) {
					fs.add(outFacts.get(p));
				}
			}
			Iterator<F> fIt = fs.iterator();
			F curIn = copyFact(fIt.next());
			while(fIt.hasNext()) {
				curIn = merge(curIn, fIt.next());
			}
			return curIn;
		}
	}

	protected abstract Map<InstructionHandle, F> getInputFacts();

	protected abstract F transfer(InstructionHandle ins, F curFact);

	protected abstract F emptyFact();

	protected abstract F copyFact(F f);

	protected abstract F merge(F in1, F in2);
}
