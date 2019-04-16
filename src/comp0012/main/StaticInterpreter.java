package comp0012.main;

import org.apache.bcel.generic.*;

import comp0012.main.Value.ConstantValue;
import comp0012.main.Value.TopValue;

public class StaticInterpreter {

	public static Frame execute(Frame f, ConstantPoolGen cpg, InstructionHandle ih) {
		Frame cur = new Frame(f);
		Instruction i = ih.getInstruction();
		if (i instanceof ACONST_NULL) {
			cur.push(Value.NULL_CONST, ih);
		} else if (i instanceof ArithmeticInstruction) {
			ArithmeticInstruction ai = (ArithmeticInstruction) i;
			cur.pop(2);
			cur.push(new TopValue(ai.getType(cpg)), ih);
		} else if (i instanceof ArrayInstruction) {
			ArrayInstruction ai = (ArrayInstruction) i;
			if (ai instanceof AALOAD || ai instanceof BALOAD || ai instanceof CALOAD || ai instanceof DALOAD
					|| ai instanceof FALOAD || ai instanceof IALOAD || ai instanceof LALOAD || ai instanceof SALOAD) {
				// pop: array, index, push: TOP (array val)
				cur.pop(2);
				cur.push(new TopValue(ai.getType(cpg)), ih);
			} else {
				// pop: array, index, value
				cur.pop(3);
			}
		} else if (i instanceof ARRAYLENGTH) {
			cur.pop();
			cur.push(new TopValue(Type.INT), ih);
		} else if (i instanceof ATHROW) {
			cur.pop();
		} else if (i instanceof ConstantPushInstruction) {
			ConstantPushInstruction cp = (ConstantPushInstruction) i;
			cur.push(new ConstantValue(cp.getType(cpg), cp.getValue()), ih);
		} else if (i instanceof BranchInstruction) {
			if (i instanceof GotoInstruction) {
				//
			} else if (i instanceof IfInstruction) {
				if (i instanceof IFEQ || i instanceof IFGE || i instanceof IFGT || i instanceof IFLE
						|| i instanceof IFLT || i instanceof IFNE || i instanceof IFNONNULL || i instanceof IFNULL) {
					cur.pop();
				} else {
					cur.pop(2);
				}
			} else if (i instanceof Select) {
				cur.pop();
			} else {
				throw new UnsupportedOperationException(i.getName());
			}
		} else if (i instanceof ConversionInstruction) {
			Value v = cur.pop();
			Type type = ((ConversionInstruction) i).getType(cpg);
			if (v instanceof ConstantValue) {
				Number n = (Number) ((ConstantValue) v).getConstant();
				if (type.equals(Type.FLOAT)) {
					cur.push(new ConstantValue(type, n.floatValue()), ih);
				} else if (type.equals(Type.INT)) {
					cur.push(new ConstantValue(type, n.intValue()), ih);
				} else if (type.equals(Type.DOUBLE)) {
					cur.push(new ConstantValue(type, n.doubleValue()), ih);
				} else if (type.equals(Type.LONG)) {
					cur.push(new ConstantValue(type, n.longValue()), ih);
				} else {
					throw new IllegalArgumentException(type.toString());
				}
			} else {
				cur.push(new TopValue(type), ih);
			}
		} else if (i instanceof CPInstruction) {
			Type type = ((CPInstruction) i).getType(cpg);
			if(i instanceof NEW) {
				cur.push(new TopValue(type), ih);
			} else if (i instanceof ANEWARRAY || i instanceof CHECKCAST || i instanceof INSTANCEOF) {
				cur.pop();
				cur.push(new TopValue(type), ih);
			} else if (i instanceof FieldOrMethod) {
				if (i instanceof FieldInstruction) {
					if (i instanceof GETFIELD) {
						cur.pop();
						cur.push(new TopValue(type), ih);
					} else if (i instanceof GETSTATIC) {
						cur.push(new TopValue(type), ih);
					} else if (i instanceof PUTFIELD) {
						cur.pop(2);
					} else if (i instanceof PUTSTATIC) {
						cur.pop();
					}
				} else if (i instanceof InvokeInstruction) {
					if (i instanceof INVOKEDYNAMIC) {
						throw new UnsupportedOperationException(i.getName());
					}
					if (i instanceof INVOKEVIRTUAL || i instanceof INVOKEINTERFACE) {
						cur.pop(); // instance
					}
					InvokeInstruction invoke = (InvokeInstruction) i;
					for (int j = 0; j < invoke.getArgumentTypes(cpg).length; j++) {
						cur.pop(); // args
					}
					Type retType = invoke.getReturnType(cpg);
					if (!retType.equals(Type.VOID)) {
						cur.push(new TopValue(retType), ih);
					}
				}
			} else if (i instanceof LDC) {
				LDC ldc = (LDC) i;
				Object o = ldc.getValue(cpg);
				if (o instanceof Number) {
					cur.push(new ConstantValue(type, 0), ih);
				} else {
					cur.push(new TopValue(type), ih);
				}
			} else if (i instanceof LDC2_W) {
				LDC2_W ldc = (LDC2_W) i;
				cur.push(new ConstantValue(type, ldc.getValue(cpg)), ih);
			} else if (i instanceof MULTIANEWARRAY) {
				MULTIANEWARRAY main = (MULTIANEWARRAY) i;
				for (int j = 0; j < main.getDimensions(); j++) {
					cur.pop();
				}
				cur.push(new TopValue(type), ih);
			}
		} else if (i instanceof DCMPG || i instanceof DCMPL || i instanceof FCMPG || i instanceof FCMPL
				|| i instanceof LCMP) {
			cur.pop(2);
			TypedInstruction ti = (TypedInstruction) i;
			cur.push(new TopValue(ti.getType(cpg)), ih);
		} else if(i instanceof MONITORENTER || i instanceof MONITOREXIT) {
			cur.pop();
		} else if(i instanceof NEWARRAY) {
			cur.pop();
			cur.push(new TopValue(((NEWARRAY) i).getType()), ih);
		} else if(i instanceof NOP) {
			
		} else if(i instanceof ReturnInstruction) {
			ReturnInstruction ri = (ReturnInstruction) i;
			if(!ri.getType().equals(Type.VOID)) {
				cur.pop();
			}
		} else if(i instanceof LocalVariableInstruction) {
			LocalVariableInstruction lvi = (LocalVariableInstruction) i;
			if(lvi instanceof LoadInstruction) {
				cur.push(cur.getLocal(lvi.getIndex()), ih);
			} else if(lvi instanceof StoreInstruction) {
				cur.setLocal(lvi.getIndex(), cur.pop());
			} else if(lvi instanceof IINC) {
				cur.setLocal(lvi.getIndex(), new TopValue(Type.INT));
			}
		} else if(i instanceof StackInstruction) {
			if(i instanceof DUP) {
				cur.push(cur.peek(), ih);
			} else if(i instanceof SWAP) {
				Value v1 = cur.pop();
				Value v2 = cur.pop();
				cur.push(v2, ih);
				cur.push(v1, ih);
			} else if(i instanceof POP) {
				cur.pop();
			} else if(i instanceof POP2) {
				if(cur.peek().getType().getSize() == 2) {
					cur.pop();
				} else {
					cur.pop(2);
				}
			} else {
				// other ones unlikely to come up
				throw new UnsupportedOperationException(i.getName());
			}
		} else {
			throw new UnsupportedOperationException(i.getName());
		}
		return cur;
	}
}
