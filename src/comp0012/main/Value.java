package comp0012.main;

import java.util.Objects;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.Type;

public interface Value {
	NullConstValue NULL_CONST = new NullConstValue();
	
	Type getType();
	
	Value merge(Value v);
	
	class NullConstValue implements Value {
		@Override
		public Type getType() {
			return Type.NULL;
		}
		
		@Override
		public Value merge(Value v) {
			if(v == this) {
				return this;
			} else {
				return new TopValue(v.getType());
			}
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof NullConstValue;
		}
		
		@Override
		public int hashCode() {
			return 13;
		}
		
		@Override
		public String toString() {
			return "nullconst";
		}
	}
	
	public class TopValue implements Value {
		private final Type type;
		
		public TopValue(Type type) {
			this.type = type;
		}

		@Override
		public Type getType() {
			return type;
		}
		
		@Override
		public Value merge(Value v) {
			if(v.getType().equals(type)) {
				return this;
			} else {
				throw new UnsupportedOperationException();
			}
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof TopValue) {
				TopValue v2 = (TopValue) o;
				return Objects.equals(v2.type, type);
			} else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(type);
		}
		
		@Override
		public String toString() {
			return "TOP :: " + type;
		}
	}
	
	public class ConstantValue implements Value {
		private final Type type;
		private final Object cst;
		
		public ConstantValue(Type type, Object cst) {
			this.type = type;
			this.cst = cst;
		}

		@Override
		public Value merge(Value v) {
			if(v.getType().equals(type)) {
				throw new UnsupportedOperationException();
			}
			if(v instanceof ConstantValue) {
				ConstantValue cv = (ConstantValue) v;
				if(cv.cst.equals(cst)) {
					return this;
				} else {
					return new TopValue(type);
				}
			} else {
				return new TopValue(type);
			}
		}
		
		@Override
		public Type getType() {
			return type;
		}
		
		public Object getConstant() {
			return cst;
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ConstantValue) {
				ConstantValue cv = (ConstantValue) o;
				return Objects.equals(cst, cv.cst) && Objects.equals(type, cv.type);
			} else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(cst, type);
		}
		
		@Override
		public String toString() {
			return cst + " :: " + type;
		}
	}
	
	public class ProducedValue implements Value {
		private final Value val;
		private final InstructionHandle ih;
		public ProducedValue(Value val, InstructionHandle ih) {
			this.val = val;
			this.ih = ih;
		}
		
		public InstructionHandle getProducer() {
			return ih;
		}
		
		public Value getInnerValue() {
			return val;
		}
		
		public Value getCoreValue() {
			if(val instanceof ProducedValue) {
				return ((ProducedValue) val).getCoreValue();
			} else {
				return val;
			}
		}
		
		@Override
		public Type getType() {
			return val.getType();
		}

		@Override
		public Value merge(Value v) {
			return val.merge(v);
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ProducedValue) {
				ProducedValue pv = (ProducedValue) o;
				return Objects.equals(pv.val, val) && Objects.equals(pv.ih, ih);
			} else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(val, ih);
		}
		
		@Override
		public String toString() {
			return val + " @ " + ih;
		}
	}
}