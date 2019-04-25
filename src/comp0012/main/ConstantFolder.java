package comp0012.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;
import org.apache.bcel.classfile.StackMapType;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.DCMPL;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.FCMPL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LineNumberGen;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;
import org.apache.bcel.verifier.structurals.OperandStack;
import org.plumelib.bcelutil.InstructionListUtils;
import org.plumelib.bcelutil.StackTypes;

import comp0012.main.Value.ConstantValue;
import comp0012.main.Value.ProducedValue;

public class ConstantFolder extends InstructionListUtils {

	ClassGen gen = null;
	
	public ConstantFolder(String file) throws ClassFormatException, IOException {
		ClassParser parser = new ClassParser(file);
		JavaClass original = parser.parse();
		gen = new ClassGen(original);
	}
	
	public void write(String optimisedFilePath) {
		optimise(gen);
		JavaClass optimized = gen.getJavaClass();
		try {
			FileOutputStream out = new FileOutputStream(
					new File(optimisedFilePath));
			optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void optimise(ClassGen cg) {
		for(Method m : cg.getMethods()) {
			try {
				cg.setMajor(50);
				cg.setMinor(0);
				optimise(cg, m);
			} catch(Throwable t) {
				System.err.println("Couldn't optimise " + cg.getClassName() + "." + m.getName() + ": " + t.getMessage());
				t.printStackTrace();
			}
		}
	}
	
	public void optimise(ClassGen cg, Method m) {
		pool = cg.getConstantPool();
		if(m.getExceptionTable() != null && m.getExceptionTable().getNumberOfExceptions() > 0) {
			System.out.printf("Can't optimise %s.%s (exception control flow).\n", cg.getClassName(), m.getName());
			return;
		}
		if(m.getCode() == null) {
			System.out.printf("Skipping %s.%s (no code).\n", cg.getClassName(), m.getName());
			return;
		}
//		if(!cg.getClassName().equals("comp0012.target.ConstantVariableFolding") || !m.getName().equals("methodThree")) {
//			return;
//		}
		System.out.printf("Processing: %s.%s %s.\n", cg.getClassName(), m.getName(), m.getSignature());
		MethodGen mg = new MethodGen(m, cg.getClassName(), pool);
		set_current_stack_map_table(mg, cg.getMajor());
		fix_local_variable_table(mg);
		build_unitialized_NEW_map(mg.getInstructionList());
		
		boolean change;
		do {
			change = false;
			change |= propagateConstants(mg);
			
			change |= foldConstants(mg);
			change |= removeDeadAssignments(mg);
			
		} while(change);
		
		mg.setMaxStack();
		mg.setMaxLocals();
		StackMap sm = new StackMap(pool.addUtf8("StackMapTable"), 0, (StackMapEntry[]) null, pool.getConstantPool());
		sm.setStackMap(stack_map_table);
		mg.addCodeAttribute(sm);
		mg.update();
		
		Method m2 = mg.getMethod();
		cg.replaceMethod(m, m2);
	}
	
	private boolean propagateConstants(MethodGen mg) {
		FrameAnalysis fa = new FrameAnalysis(mg);
		fa.solve();
		
		InstructionList list = mg.getInstructionList();
		boolean change = false;
		
		for (InstructionHandle ih : list.getInstructionHandles()) {
			Instruction i = ih.getInstruction();
			if(i instanceof LoadInstruction) {
				LoadInstruction li = (LoadInstruction) i;
				Value local = fa.getInFact(ih).getLocal(li.getIndex());
				ConstantValue constLocal = asConstantValue(local);
				if(constLocal != null) {
					Object cst = constLocal.getConstant();
					if(cst instanceof Number) {
						change |= toInsn(mg, ih, (Number) cst);
					}
				}
			}
		}
		return change;
	}
	
	private boolean foldConstants(MethodGen mg) {
		FrameAnalysis fa = new FrameAnalysis(mg);
		fa.solve();
		
		InstructionList list = mg.getInstructionList();
		boolean change = false;
		for (InstructionHandle ih : list.getInstructionHandles()) {
			Instruction i = ih.getInstruction();
			int op = i.getOpcode();
			Frame inFrame = fa.getInFact(ih);
			
			Number res = null;
			int pops = 0;
			
			if(i instanceof ArithmeticInstruction) {
				if(op >= 0x74 && op <= 0x77) {
					// INEG, FNEG, DNEG, LNEG are unary
					ConstantValue cv = asConstantValue(inFrame.peek());
					if(cv != null) {
						foldConstantNegation(op, cv);
						pops = 1;
					}
				} else {
					ConstantValue cv2 = asConstantValue(inFrame.peek());
					ConstantValue cv1 = asConstantValue(inFrame.peek(1));
					if(cv1 != null && cv2 != null) {
						res = foldBinaryArithmeticOp(op, cv1, cv2);
						pops = 2;
					}
				}
			} else if(i instanceof ConversionInstruction) {
				ConversionInstruction ci = (ConversionInstruction) i;
				ConstantValue cv = asConstantValue(inFrame.peek());
				if(cv != null) {
					res = castConstant(cv, ci.getType(pool));
					pops = 1;
				}
			} else if(i instanceof IfInstruction) {
				IfInstruction ifi = (IfInstruction) i;
				boolean decided = false;
				boolean decision = false;
				if(op >= 0x99 && op <= 0x9e) {
					ConstantValue cv = asConstantValue(inFrame.peek());
					if(cv != null) {
						decided = true;
						decision = unaryConstantBranch(op, cv);
						pops = 1;
					}
				} else {
					if((op >= 0xa0 && op <= 0xa4) || op == 0x9f) {
						ConstantValue cv2 = asConstantValue(inFrame.peek());
						ConstantValue cv1 = asConstantValue(inFrame.peek(1));
						if(cv1 != null && cv2 != null) {
							decided = true;
							decision = binaryConstantBranch(op, cv1, cv2);
							pops = 2;
						}
					}
				}
				// Handle the actual modification here and pop args as usual below
				if(decided) {
					if(decision) {
						InstructionList il2 = new InstructionList(InstructionFactory.createBranchInstruction((short)0xa7, ifi.getTarget()));
						InstructionHandle p = ih.getPrev();
						delete_instructions(mg, ih, ih);
						insert_before_handle(mg, p, il2, false);
					} else {
						delete_instructions(mg, ih, ih);
					}
				}
			} else if(i instanceof DCMPG || i instanceof DCMPL) {
				ConstantValue cv2 = asConstantValue(inFrame.peek());
				ConstantValue cv1 = asConstantValue(inFrame.peek(1));
				if(cv1 != null && cv2 != null) {
					res = dcmp(cv1, cv2, i instanceof DCMPG ? 1 : -1);
					pops = 2;
				}
			} else if(i instanceof FCMPG || i instanceof FCMPL) {
				ConstantValue cv2 = asConstantValue(inFrame.peek());
				ConstantValue cv1 = asConstantValue(inFrame.peek(1));
				if(cv1 != null && cv2 != null) {
					res = fcmp(cv1, cv2, i instanceof FCMPG ? 1 : -1);
					pops = 2;
				}
			} else if(i instanceof LCMP) {
				ConstantValue cv2 = asConstantValue(inFrame.peek());
				ConstantValue cv1 = asConstantValue(inFrame.peek(1));
				if(cv1 != null && cv2 != null) {
					res = lcmp(cv1, cv2);
					pops = 2;
				}
			}
			if(res != null) {
				toInsn(mg, ih, res);
			}

			// We need to pop the operands off the stack but for
			// simplicity sake wrt control flow, we assume they are constant ProducedValues
			// and we remove the producers of the values.
			for(int j=0; j < pops; j++) {
				removeProducers(mg, (ProducedValue) inFrame.peek(j));
			}
			
			change |= (res != null || pops > 0);
		}
		return change;
	}
		
	private boolean removeDeadAssignments(MethodGen mg) {
		LivenessAnalysis la = new LivenessAnalysis(mg);
		la.solve();
		
		FrameAnalysis fa = new FrameAnalysis(mg);
		fa.solve();

		boolean change = false;
		InstructionList list = mg.getInstructionList();
		for (InstructionHandle ih : list.getInstructionHandles()) {
			Instruction i = ih.getInstruction();
			if(i instanceof StoreInstruction) {
				StoreInstruction si = (StoreInstruction) i;
				Set<Integer> liveOut = la.getOutFact(ih);
				/* assignment is unused, remove it if we can */
				if(!liveOut.contains(si.getIndex())) {
					/* The top element on the stack of the pre-execution frame
					 * contains the value that will be stored when this instruction is executed.
					 * For now, we can remove it if it's a constant (TODO: simple side effect decisions). */
					Frame inFrame = fa.getInFact(ih);
					ProducedValue pv = (ProducedValue) inFrame.peek();
					Value v = pv.getCoreValue();
					if(v instanceof ConstantValue) {
						removeProducers(mg, pv);
						delete_instructions(mg, ih, ih);
						change = true;
					}
				}
			}
		}
		
		return change;
	}
	
	private void removeProducers(MethodGen mg, ProducedValue pv) {
		for(InstructionHandle ih : pv.getProducers()) {
			delete_instructions(mg, ih, ih);
		}
	}
	
	private Number lcmp(ConstantValue cv1, ConstantValue cv2) {
		long n1 = ((Number) cv1.getConstant()).longValue();
		long n2 = ((Number) cv2.getConstant()).longValue();
		return Long.compare(n1, n2);
	}
	
	private Number fcmp(ConstantValue cv1, ConstantValue cv2, int b) {
		float n1 = ((Number) cv1.getConstant()).floatValue();
		float n2 = ((Number) cv2.getConstant()).floatValue();
		if(Float.isNaN(n2) || Float.isNaN(n2)) {
			return b;
		} else {
			return (int)Math.signum(Float.compare(n1, n2));
		}
	}
	
	private Number dcmp(ConstantValue cv1, ConstantValue cv2, int b) {
		double n1 = ((Number) cv1.getConstant()).doubleValue();
		double n2 = ((Number) cv2.getConstant()).doubleValue();
		if(Double.isNaN(n1) || Double.isNaN(n1)) {
			return b;
		} else {
			return (int)Math.signum(Double.compare(n1, n2));
		}
	}
	
	private boolean unaryConstantBranch(int op, ConstantValue cv) {
		Number n = (Number) cv.getConstant();
		switch(op) {
			case 0x99: // IFEQ
				return n.intValue() == 0;
			case 0x9c: // IFGE
				return n.intValue() >= 0;
			case 0x9d: // IFGT
				return n.intValue() > 0;
			case 0x9e: // IFLE
				return n.intValue() <= 0;
			case 0x9b: // IFLT
				return n.intValue() < 0;
			case 0x9a: // IFNE
				return n.intValue() != 0;
			default:
				throw new UnsupportedOperationException("0x" + op);
		}
	}
	
	private boolean binaryConstantBranch(int op, ConstantValue cv1, ConstantValue cv2) {
		Number n1 = (Number) cv1.getConstant();
		Number n2 = (Number) cv2.getConstant();
		switch(op) {
			case 0x9f: // IF_ICMPEQ
				return n1.intValue() == n2.intValue();
			case 0xa0: // IF_ICMPNE
				return n1.intValue() != n2.intValue();
			case 0xa1: // IF_ICMPLT
				return n1.intValue() < n2.intValue();
			case 0xa2: // IF_ICMPGE
				return n1.intValue() >= n2.intValue();
			case 0xa3: // IF_ICMPLGT
				return n1.intValue() > n2.intValue();
			case 0xa4: // IF_ICMPLE
				return n1.intValue() <= n2.intValue();
			default:
				throw new UnsupportedOperationException("0x" + op);
		}
	}
	
	private Number castConstant(ConstantValue cv, Type to) {
		Number n = (Number) cv.getConstant();
		if(to == Type.FLOAT) {
			return n.floatValue();
		} else if(to == Type.DOUBLE) {
			return n.doubleValue();
		} else if(to == Type.INT) {
			return n.intValue();
		} else if(to == Type.LONG) {
			return n.longValue();
		} else {
			throw new UnsupportedOperationException(to.toString());
		}
	}
	
	private Number foldBinaryArithmeticOp(int op, ConstantValue cv1, ConstantValue cv2) {
		Number n1 = (Number) cv1.getConstant();
		Number n2 = (Number) cv2.getConstant();
		Number res;
		switch(op) {
			case 0x63: // DADD
				res = n1.doubleValue() + n2.doubleValue();
				break;
			case 0x6f: // DDIV:
				res = n1.doubleValue() / n2.doubleValue();
				break;
			case 0x6b: // DMUL
				res = n1.doubleValue() * n2.doubleValue();
				break;
			case 0x73: // DREM
				res = n1.doubleValue() % n2.doubleValue();
				break;
			case 0x67: // DSUB
				res = n1.doubleValue() - n2.doubleValue();
				break;
			case 0x62: // FADD
				res = n1.floatValue() + n2.floatValue();
				break;
			case 0x6e: // FDIV
				res = n1.floatValue() / n2.floatValue();
				break;
			case 0x6a: // FMUL
				res = n1.floatValue() * n2.floatValue();
				break;
			case 0x72: // FREM
				res = n1.floatValue() % n2.floatValue();
				break;
			case 0x66: // FSUB
				res = n1.floatValue() - n2.floatValue();
				break;
			case 0x60: // IADD
				res = n1.intValue() + n2.intValue();
				break;
			case 0x7e: // IAND
				res = n1.intValue() & n2.intValue();
				break;
			case 0x6c: // IDIV
				res = n1.intValue() / n2.intValue();
				break;
			case 0x68: // IMUL
				res = n1.intValue() * n2.intValue();
				break;
			case 0x80: // IOR
				res = n1.intValue() | n2.intValue();
				break;
			case 0x70: // IREM
				res = n1.intValue() % n2.intValue();
				break;
			case 0x78: // ISHL
				res = n1.intValue() << n2.intValue();
				break;
			case 0x7a: // ISHR
				res = n1.intValue() >> n2.intValue();
				break;
			case 0x64: // ISUB
				res = n1.intValue() - n2.intValue();
				break;
			case 0x7c: // IUSHR
				res = n1.intValue() >>> n2.intValue();
				break;
			case 0x82: // IXOR
				res = n1.intValue() ^ n2.intValue();
				break;
			case 0x61: // LADD
				res = n1.longValue() + n2.longValue();
			case 0x7f: // LAND
				res = n1.longValue() & n2.longValue();
				break;
			case 0x6d: // LDIV
				res = n1.longValue() / n2.longValue();
				break;
			case 0x69: // LMUL
				res = n1.longValue() * n2.longValue();
				break;
			case 0x81: // LOR
				res = n1.longValue() | n2.longValue();
				break;
			case 0x71: // LREM
				res = n1.longValue() % n2.longValue();
				break;
			case 0x79: // LSHL
				res = n1.longValue() << n2.longValue();
				break;
			case 0x7b: // LSHR
				res = n1.longValue() >> n2.longValue();
				break;
			case 0x65: // LSUB
				res = n1.longValue() - n2.longValue();
				break;
			case 0x7d: // LUSHR
				res = n1.longValue() >>> n2.longValue();
				break;
			case 0x83: // LXOR
				res = n1.longValue() ^ n2.longValue();
				break;
			default:
				throw new UnsupportedOperationException("0x" + op);
		}
		return res;
	}
	
	private Number foldConstantNegation(int op, ConstantValue cv) {
		Number n = (Number) cv.getConstant();
		Number res;
		switch(op) {
			case 0x74:
				res = -n.intValue();
				break;
			case 0x75:
				res = -n.longValue();
				break;
			case 0x76:
				res = -n.floatValue();
				break;
			case 0x77:
				res = -n.doubleValue();
				break;
			default:
				throw new IllegalArgumentException("0x" + op);
		}
		return res;
	}
	
	private boolean toInsn(MethodGen mg, InstructionHandle ih, Number n) {
		Instruction i;
		if(n instanceof Float) {
			i = new LDC(pool.addFloat(n.floatValue()));
		} else if(n instanceof Integer) {
			i = new LDC(pool.addInteger(n.intValue()));
		} else if(n instanceof Double) {
			i = new LDC2_W(pool.addDouble(n.doubleValue()));
		} else if(n instanceof Long) {
			i = new LDC2_W(pool.addLong(n.longValue()));
		} else {
			return false;
		}
		replace_instructions(mg, mg.getInstructionList(), ih, build_il(i));
		return true;
	}
	
	private ConstantValue asConstantValue(Value v) {
		/* If it's not produced, it was defined as a parameter */
		if(v instanceof ProducedValue) {
			v = ((ProducedValue) v).getCoreValue();
		}
		if(v instanceof ConstantValue) {
			return (ConstantValue) v;
		} else {
			return null;
		}
	}
}
