package comp0012.main;

import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.BALOAD;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.CALOAD;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.DALOAD;
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.DCMPL;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.FALOAD;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.FCMPL;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.FieldOrMethod;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IALOAD;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.IFGE;
import org.apache.bcel.generic.IFGT;
import org.apache.bcel.generic.IFLE;
import org.apache.bcel.generic.IFLT;
import org.apache.bcel.generic.IFNE;
import org.apache.bcel.generic.IFNONNULL;
import org.apache.bcel.generic.IFNULL;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LALOAD;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.POP2;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.SALOAD;
import org.apache.bcel.generic.SWAP;
import org.apache.bcel.generic.Select;
import org.apache.bcel.generic.StackInstruction;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;

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
					if (i instanceof INVOKEVIRTUAL || i instanceof INVOKEINTERFACE || i instanceof INVOKESPECIAL) {
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
					cur.push(new ConstantValue(type, o), ih);
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
