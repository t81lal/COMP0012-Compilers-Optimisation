package comp0012.main;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import comp0012.main.Value.ProducedValue;
import comp0012.main.Value.TopValue;

/**
 * Represents the state of the local variables and execution stack of the JVM at a single point in time. <br>
 * The runtime values of variables and stack slots are symbolically represented using {@link Value}'s.
 */
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
	
	public Value peek(int i) {
		return stack.get(i);
	}

	public Value peek() {
		return stack.peek();
	}

	public void push(Value v) {
		stack.push(v);
	}

	public void push(Value v, InstructionHandle pos) {
		push(new ProducedValue(v, Collections.singleton(pos)));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Locals:\n");
		for (int i = 0; i < locals.length; i++) {
			sb.append(i).append(" :: ").append(locals[i]).append("\n");
		}
		sb.append("Stack:\n");
		for (Value v : stack) {
			sb.append(v).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Generate the initial frame on entry to a method.
	 * @param mg The method to generate the frame for.
	 * @return The frame.
	 */
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
//			System.err.println("F1:");
//			System.err.print(f1);
//			System.err.println("===================\nF2:");
//			System.err.print(f2);
			throw new IllegalArgumentException("Stack height mismatch");
		} else {
			for (int i = 0; i < f1.stack.size(); i++) {
				merged.stack.push(f1.stack.get(i).merge(f2.stack.get(i)));
			}
		}
		for (int i = 0; i < merged.locals.length; i++) {
			Value l1 = f1.locals[i],
				  l2 = f2.locals[i];
			if(l1 != null && l2 != null) {
				merged.locals[i] = l1.merge(l2);
			} else if(l1 == null) {
				merged.locals[i] = l2;
			} else if(l2 == null) {
				merged.locals[i] = l1;
			}
		}
		return merged;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Frame) {
			Frame f = (Frame) o;
			return Arrays.equals(locals, f.locals) && Objects.equals(stack, f.stack);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(locals, stack);
	}
}
