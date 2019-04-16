package comp0012.main;

import java.util.Arrays;
import java.util.LinkedList;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import comp0012.main.Value.ProducedValue;
import comp0012.main.Value.TopValue;

public class Frame {

	final Value[] locals;
	final LinkedList<Value> stack;

	public Frame(int maxLocals) {
		locals = new Value[maxLocals];
		stack = new LinkedList<>();
	}

	public Frame(Frame other) {
		locals = Arrays.copyOf(other.locals, other.locals.length);
		stack = new LinkedList<>(other.stack);
	}

	public Value getLocal(int i) {
		return locals[i];
	}

	public void setLocal(int i, Value v) {
		locals[i] = v;
	}

	public void pop(int n) {
		for (int i = 0; i < n; i++) {
			pop();
		}
	}

	public Value pop() {
		return stack.pop();
	}

	public Value peek() {
		return stack.peek();
	}

	public void push(Value v) {
		stack.push(v);
	}
	
	public void push(Value v, InstructionHandle pos) {
		push(new ProducedValue(v, pos));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Locals:\n");
		for(int i=0; i < locals.length; i++) {
			sb.append(i).append(" :: ").append(locals[i]).append("\n");
		}
		sb.append("Stack:\n");
		for(Value v : stack) {
			sb.append(v).append("\n");
		}
		return sb.toString();
	}

	public static Frame makeFrame(MethodGen mg) {
		Frame frame0 = new Frame(mg.getMaxLocals());
		int idx = 0;
		if (!mg.isStatic()) {
			frame0.setLocal(idx++, new TopValue(new ObjectType(mg.getClassName())));
		}
		Type[] args = mg.getArgumentTypes();
		for (int i = 0; i < args.length; i++) {
			frame0.setLocal(idx, new TopValue(args[i]));
			idx += args[i].getSize();
		}
		return frame0;
	}
	
	public static Frame merge(Frame f1, Frame f2) {
		Frame merged = new Frame(f1.locals.length);
		if (f1.stack.size() != f2.stack.size()) {
			throw new IllegalArgumentException();
		} else {
			for (int i = 0; i < f1.stack.size(); i++) {
				merged.stack.set(i, f1.stack.get(i).merge(f2.stack.get(i)));
			}
		}
		for (int i = 0; i < merged.locals.length; i++) {
			merged.locals[i] = f1.locals[i].merge(f2.locals[i]);
		}
		return merged;
	}
}
