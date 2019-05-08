import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ST {

	
	public static String indent(int n) {
		return "  " + Integer.toString(n);
	}
	
	public static class Var{
		
		String name;
		int lev;
		String tpName;
		private Class<?> tp;
		private Object tpObj;
	    int size;
	    int offset;
	    int adr;
		int tp_size;
	    int tp_adr;
	    int tp_base_size;
	    int tp_lower;
	    int tp_length;
	    Object tp_base;
		public Var(Class<?> tp){
			this.tp = tp;
			this.tpName = (tp == null) ? "" : tp.getSimpleName();
		}
		
		public Var(Object tpObj){
			this.tpObj = tpObj;
			if (tpObj.toString().equals("Array") || tpObj.toString().equals("Record")) {
				this.tpName = (tpObj.toString()).substring(0, (tpObj.toString()).indexOf("("));
			}else {
				this.tpName = tpObj.toString();
			}
//			tpObj.toString().substring(tpObj.toString().indexOf("base = ")+7);
			//TODO to for record
			if (this.tpName.startsWith("Array")) {
				this.tp_length = Integer.parseInt(tpObj.toString().substring(tpObj.toString().indexOf("length = ")+9, tpObj.toString().indexOf(", base")));
				this.tp_lower = Integer.parseInt(tpObj.toString().substring(tpObj.toString().indexOf("lower = ")+8, tpObj.toString().indexOf(",")));
				this.tp_base = tpObj.toString().substring(tpObj.toString().indexOf("base = ")+7,tpObj.toString().length()-1);
				if (this.tp_base.toString().startsWith("Array") && !this.tp_base.toString().contains("Record"))
					this.tp_base_size = Integer.parseInt(this.tp_base.toString().substring(this.tp_base.toString().indexOf("length = ")+9, this.tp_base.toString().lastIndexOf(",")));
				else if (this.tp_base.toString().equals("Int") || this.tp_base.toString().equals("Bool")) 
					this.tp_base_size = 1;
				else 
					this.tp_base_size = Helper.getFieldProp(this.tpName).length;
				
					
			}
		}
		public Object get_tp_base() {
			return this.tp_base;
		}
		public void set_tp_base(Object tp_base) {
			this.tp_base = tp_base;
		}
		
		public int get_tp_length() {
			return this.tp_length;
		}

		public void set_tp_length(int tp_length) {
			this.tp_length = tp_length;
		}
		
		public int get_tp_lower() {
			return this.tp_lower;
		}

		public void set_tp_lower(int tp_lower) {
			this.tp_lower = tp_lower;
		}
		
	    public void set_tp_size(int tp_size) {
			this.tp_size = tp_size;
		}
	    
		public int get_tp_size() {
			return this.tp_size;
		}
	    
		public int get_tp_adr() {
			return this.tp_adr;
		}
		
		public void set_tp_adr(int tp_adr) {
			this.tp_adr = tp_adr;
		}
		
		public int get_tp_base_size() {
			return this.tp_base_size;
		}
	    
		public void set_tp_base_size(int tp_base_size) {
			this.tp_base_size = tp_base_size;
		}
		
		public void setTPObj(String tpObj) {
			this.tpObj = tpObj;
		}
		public Object getTPObj() {
			return this.tpObj;
		}

		public String toString() {
			return "Var(name = " + getName() +  ", lev = " +  getLev() + ", tp = " + ( (this.tpObj==null) ? this.tpName : this.tpObj ) + ")";
		}
		
		public void setLev(int lev) {
			this.lev = lev;
		}
		
		public int getLev() {
			return this.lev;
		}

		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String getTP() {
			return this.tpName;
		}
		
		public void setTP(String tpName) {
			this.tpName = tpName;
		}

		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
	}
	
	public static class Ref{
		
		String name;
		int lev;
		String tpName;
	    int size;
	    int offset;
	    int adr;
		private Class<?> tp;
		private Object tpObj;
		int tp_size;
	    int tp_adr;
	    int tp_base_size;
	    int tp_lower;
	    int tp_length;
	    Object tp_base;
		
	    public Ref(Class<?> tp){
			this.tp = tp;
			setTP((tp == null) ? "" : tp.getSimpleName());
		}
		
		public Ref(Object tpObj){
			this.tpObj = tpObj;
//			this.tpName = ((String)tpObj).substring(0, ((String)tpObj).indexOf("("));
			if (tpObj.toString().equals("Array") || tpObj.toString().equals("Record")) {
				this.tpName = (tpObj.toString()).substring(0, (tpObj.toString()).indexOf("("));
			}else {
				this.tpName = tpObj.toString();
			}
			
//			tpObj.toString().substring(tpObj.toString().indexOf("base = ")+7);
			//TODO to for record
			if (this.tpName.startsWith("Array")) {
				this.tp_length = Integer.parseInt(tpObj.toString().substring(tpObj.toString().indexOf("length = ")+9, tpObj.toString().indexOf(", base")));
				this.tp_lower = Integer.parseInt(tpObj.toString().substring(tpObj.toString().indexOf("lower = ")+8, tpObj.toString().indexOf(",")));
				this.tp_base = tpObj.toString().substring(tpObj.toString().indexOf("base = ")+7,tpObj.toString().length()-1);
				if (this.tp_base.toString().startsWith("Array") && !this.tp_base.toString().contains("Record"))
					this.tp_base_size = Integer.parseInt(this.tp_base.toString().substring(this.tp_base.toString().indexOf("length = ")+9, this.tp_base.toString().lastIndexOf(",")));
				else if (this.tp_base.toString().equals("Int") || this.tp_base.toString().equals("Bool")) 
					this.tp_base_size = 1;
				else 
					this.tp_base_size = Helper.getFieldProp(this.tpName).length;
					
			}
		}
		
		public String toString() {
			return "Ref(name = " + getName() +  ", lev = " +  getLev() + ", tp = " + this.tpObj+ ")";
		}
	    
		public int get_tp_length() {
			return this.tp_length;
		}

		public void set_tp_length(int tp_length) {
			this.tp_length = tp_length;
		}
		
		public int get_tp_lower() {
			return this.tp_lower;
		}

		public void set_tp_lower(int tp_lower) {
			this.tp_lower = tp_lower;
		}
		
		public Object get_tp_base() {
			return this.tp_base;
		}
		public void set_tp_base(Object tp_base) {
			this.tp_base = tp_base;
		}
	    public void set_tp_size(int tp_size) {
			this.tp_size = tp_size;
		}
	    
		public int get_tp_size() {
			return this.tp_size;
		}
	    
		public int get_tp_adr() {
			return this.tp_adr;
		}
		
		public void set_tp_adr(int tp_adr) {
			this.tp_adr = tp_adr;
		}
		
		public int get_tp_base_size() {
			return this.tp_base_size;
		}
	    
		public void set_tp_base_size(int tp_base_size) {
			this.tp_base_size = tp_base_size;
		}
		
			
		public void setTPObj(String tpObj) {
			this.tpObj = tpObj;
		}
		public Object getTPObj() {
			return this.tpObj;
		}

		public void setLev(int lev) {
			this.lev = lev;
		}
		
		public int getLev() {
			return this.lev;
		}

		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String getTP() {
			return this.tpName;
		}
		
		public void setTP(String tpName) {
			this.tpName = tpName;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
		
	}
	
	public static class Const{
		
		String name;
		int val;
		public Class<?> tp;
		String tpName;
	    int size;
	    int offset;
	    int adr;
		
		public Const(Class<?> tp,int val){
			this.tp = tp;
			this.val = val;
			setTP((tp == null) ? "" : tp.getSimpleName());

		}

		public String toString() {
			return "Const(name = " + getName() + ", tp = " + getTP() + ", val = " + Integer.toString(this.val)  + ")";
		}
		
		public void setVal(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}

		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}

		public String getTP() {
			return this.tpName;
		}
		
		public void setTP(String tpName) {
			this.tpName = tpName;
		}
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
	}
	
	public static class Type{
		
		String name;
		Object val;
		String tpName;
		private Class<?> tp;
		private Object tpObj;

	    int size;
	    int offset;
	    int adr;
		
		public Type(Class<?> tp){
			this.tp = tp;
			setTP(tp.getSimpleName());
			setVal(tp.getSimpleName());
		}
		
		public Type(Object tpObj){
			this.tpObj = null;
			this.val = tpObj;
//			setTP((tpObj == null) ? "" : tpObj.substring(0, tpObj.indexOf("(")));

		}
		int tp_size;
	    int tp_adr;
	    int tp_base_size;
	    public void set_tp_size(int tp_size) {
			this.tp_size = tp_size;
		}
	    
		public int get_tp_size() {
			return this.tp_size;
		}
	    
		public int get_tp_adr() {
			return this.tp_adr;
		}
		
		public void set_tp_adr(int tp_adr) {
			this.tp_adr = tp_adr;
		}
		
		public int get_tp_base_size() {
			return this.tp_base_size;
		}
	    
		public void set_tp_base_size(int tp_base_size) {
			this.tp_base_size = tp_base_size;
		}

		public void setTPObj(String tpObj) {
			this.tpObj = tpObj;
		}
		public Object getTPObj() {
			return this.tpObj;
		}

		public String toString() {
			return "Type(name = " + getName() + ", val = " + ( (this.tpObj==null) ? this.val : this.tpObj ) +  ")";
		}
		
		public Object getVal() {
			return this.val;
		}
		
		public void setVal(Object val) {
			this.val = val;
		}

		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
		

		public String getTP() {
			return this.tpName;
		}
		
		public void setTP(String tpName) {
			this.tpName = tpName;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
		
	}
	
	public static class Proc{
		
		String name;
		int lev;
		ArrayList<?> par;
		Const constObj;
		Ref refObj;
		Var varObj;
	    int size;
	    int offset;
	    int adr;
		
		public Proc(ArrayList <?> par){
			this.par = par;
			
			
		}
		
		public void setPar(ArrayList<?> par) {
			this.par = par;
		}
		
		public ArrayList<?> getPar() {
			return this.par;
		}

		public String toString() {
			return "Proc(name = " + getName() +  ", lev = " + getLev() + ", par = " + this.par.toString() + ")";
		}
		public void setLev(int lev) {
			this.lev = lev;
		}
		
		public int getLev() {
			return this.lev;
		}

		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
		
	}
	
	public static class StdProc{
		
		String name;
		int lev;
		public String result="";
		ArrayList<?> par;
		Const constObj;
		Ref refObj;
		Var varObj;
	    int size;
	    int offset;
	    int adr;
		
		public StdProc(ArrayList <?> par){
			this.par = par;
			
//			System.out.println("par in cons " + par.toString());
			
			if ((null == par) && (par.isEmpty()) || par.toString().equals("[]")) {
				return;
			}else {
				String rawPar = (String) par.get(0);
				String className = rawPar.substring(1, rawPar.indexOf("("));
				String parName = rawPar.substring(rawPar.indexOf("(")+1, rawPar.indexOf(")"));
//				System.out.println("className " + className);
//				System.out.println("parName " + parName);
				try {
					if (className.equals("Var")) {
//						this.varObj = new Var(Class.forName("ST."+className));
						this.varObj = new Var(Int.class);

					}else if (className.equals("Ref")) {
//						this.refObj = new Ref(Class.forName("ST."+className));
						this.refObj = new Ref(Int.class);

					}
				}/*catch(ClassNotFoundException ex) {
			         System.out.println(ex.toString());
			    }*/
				catch(Exception ex) {
					
				}
			}

			if (this.varObj != null) {
				this.result = this.varObj.toString();
			}else if (this.refObj != null) {
				this.result = this.refObj.toString();
			}

		}

		public String toString() {
			return "StdProc(name = " + getName() +  ", lev = " + getLev() + ", par = [" + result + "])";
		}
		public void setLev(int lev) {
			this.lev = lev;
		}
		
		public int getLev() {
			return this.lev;
		}

		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public ArrayList <?> getPar(){
			return this.par;
		}
		
		public void setPar(ArrayList<?> par) {
			this.par = par;
		}
				
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
	}
 	
	public static class Int{
		
	    int size;
	    int offset;
	    int adr;
		int val;
		
		public int getVal() {
			return this.val;
		}
		
		public void setVal(int val) {
			this.val = val;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
	}
	
	public static class Bool{
		int val;
	    int size;
	    int offset;
	    int adr;
	    
		public int getVal() {
			return this.val;
		}
		
		public void setVal(int val) {
			this.val = val;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
	}
	
	public static class Enum{}
	
	public static class Record{
		ArrayList <?> fields;
		Var varObj;
	    int size;
	    int offset;
	    int adr;
		
	    int tp_size;
	    int tp_adr;
	    int tp_base_size;
	    
	    public void set_tp_size(int tp_size) {
			this.tp_size = tp_size;
		}
	    
		public int get_tp_size() {
			return this.tp_size;
		}
	    
		public int get_tp_adr() {
			return this.tp_adr;
		}
		
		public void set_tp_adr(int tp_adr) {
			this.tp_adr = tp_adr;
		}
		
		public int get_tp_base_size() {
			return this.tp_base_size;
		}
	    
		public void set_tp_base_size(int tp_base_size) {
			this.tp_base_size = tp_base_size;
		}
		

	    
	    
		public Record(ArrayList<?> r) {
			this.fields = r;
			
		}
		
		public String toString() {
			return "Record(fields = " + this.fields.toString();
		}
		
		public void setField(ArrayList <?> fields) {
			this.fields = fields;
		}
		
		public ArrayList<?> getField(){
			return this.fields;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
	}
	
	public static class Array{
		
		Object base;
		int lower;
		int length;
	    int size;
	    int offset;
	    int adr;
		int tp_size;
	    int tp_adr;
	    int tp_base_size;
	    
		public Array(Object base, int lower, int length) {
			this.base = base;
			this.lower = lower;
			this.length = length;
			if (base.toString().equals("Int") || base.toString().equals("Bool")) {
				tp_base_size = 1;
			}
			
		}
		
		public String toString() {
			return "Array(lower = " + lower + ", length = " + length + ", base = " + this.base.toString() + ")";
		}
		
	    public void set_tp_size(int tp_size) {
			this.tp_size = tp_size;
		}
	    
		public int get_tp_size() {
			return this.tp_size;
		}
	    
		public int get_tp_adr() {
			return this.tp_adr;
		}
		
		public void set_tp_adr(int tp_adr) {
			this.tp_adr = tp_adr;
		}
		
		public int get_tp_base_size() {
			return this.tp_base_size;
		}
	    
		public void set_tp_base_size(int tp_base_size) {
			this.tp_base_size = tp_base_size;
		}
		
		public void setBase(Object base) {
			this.base = base;
		}
		
		public Object getBase() {
			return this.base;
		}
		
		public void setLower(int lower) {
			this.lower = lower;
		}
		
		public int getLower() {
			return this.lower;
		}
		public void setLength(int length) {
			this.length = length;
		}
		
		public int getLength() {
			return this.length;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public void setAdr(int adr) {
			this.adr = adr;
		}
		
		public int getAdr() {
			return this.adr;
		}
		
	}
	
	public static ArrayList<ArrayList<?>> symTab;
	public static ArrayList<Object> temp;
	public static ArrayList <Object> top;
		
	public static void init() {
		symTab = new ArrayList<ArrayList<?>>();
		top = new ArrayList<Object>();
		temp = new ArrayList<Object>();
		symTab.add(top);

	}
	
	public static void printSymTab() {
		
		for (ArrayList<?> arrayList : symTab) {
			for (Object object : arrayList) {
				System.out.println(object.toString());
			}
			System.out.println();
		}
	}
	
	public static <T> void newDecl(Object name, String entry) {
		boolean hasSubParams = false;
		
		if (symTab != null && !symTab.isEmpty())
			top =  (ArrayList<Object>) symTab.get(0);
		
		String className="";
		String params = "";
		if (entry.contains("(")) {
			className = entry.substring(0,entry.indexOf("("));
			params = entry.substring(entry.indexOf("(")+1, entry.length()-1);
		}else {
			className = entry;
		}
		
		String[] paramsList = params.split(", ");
		
		
		Type typeObj = null;
		Const constObj = null;
		Proc procObj = null;
		StdProc stdProcObj = null;
		Var varObj = null;
		Ref refObj = null;

		//TODO: add more cases for all potential use cases
		try {
			if (className.equals("Const")) {
				if (paramsList[0].equals("Bool")) {
					constObj = new Const(Bool.class,Integer.parseInt(paramsList[1]));
				}else if (paramsList[0].equals("Int")) {
					constObj = new Const(Int.class,Integer.parseInt(paramsList[1]));
				}else if (params.contains("Int")){
					constObj = new Const(Int.class,Integer.parseInt(params.substring(params.indexOf("val")+6)));
				}
			}else if (className.equals("Type")) {
				if (paramsList[0].equals("Bool")) {
					typeObj = new Type(Bool.class);
				}else if (paramsList[0].equals("Int")) {
					typeObj = new Type(Int.class);
				}else if (paramsList[1].contains("Array")) {
					typeObj = new Type(Array.class);
				}else if (paramsList[1].contains("Record")) {
					typeObj = new Type(Record.class);
				}
			}else if (className.equals("StdProc")) {
				stdProcObj = new StdProc(Helper.toList(paramsList[0]));
			}else if (className.equals("Proc")) {
				procObj = new Proc(Helper.toList(paramsList[0]));
			}else if (className.equals("Var")) {
				if (paramsList[0].equals("Bool")) {
					varObj = new Var(Bool.class);
				}else if (paramsList[0].equals("Int")) {
					varObj = new Var(Int.class);
				}else if (paramsList[0].equals("Type")) {
					varObj = new Var(Type.class);
				}else if (params.startsWith("Array")) {
					varObj = new Var(params);
				}else if (params.startsWith("Record")) {
					varObj = new Var(params);
				}
			}else if (className.equals("Ref")) {
				if (paramsList[0].equals("Bool")) {
					refObj = new Ref(Bool.class);
				}else if (paramsList[0].equals("Int")) {
					refObj = new Ref(Int.class);
				}else if (paramsList[0].equals("Type")) {
					refObj = new Ref(Type.class);
				}else if (params.startsWith("Array")) {
					refObj = new Ref(Array.class);
				}else if (params.startsWith("Record")) {
					refObj = new Ref(Record.class);
				}
			}
		}catch(Exception ex) {
	        ex.printStackTrace();
	    }
		
		Method setLevMethod = null;
		Method setNameMethod = null;
		Method setValMethod = null;

		try {
			if (typeObj != null) {
				setNameMethod = typeObj.getClass().getMethod("setName", String.class);
				setNameMethod.invoke(typeObj, name);
				
				if (params.equals("Int") || params.equals("Bool")) {
					setValMethod = typeObj.getClass().getMethod("setVal", Object.class);
					setValMethod.invoke(typeObj, params);
				}else {
					setValMethod = typeObj.getClass().getMethod("setVal", Object.class);
					setValMethod.invoke(typeObj, params.substring(params.indexOf("val =")+6,params.length()));
				}
				
			}else if (constObj != null) {
				setNameMethod = constObj.getClass().getMethod("setName", String.class);
				
				setNameMethod.invoke(constObj, name);
			}else if (procObj != null) {
				setLevMethod = procObj.getClass().getMethod("setLev", int.class);
				setNameMethod = procObj.getClass().getMethod("setName", String.class);
				
				setLevMethod.invoke(procObj, (symTab != null && symTab.size() != 0 ) ? symTab.size()-1 :0);
				setNameMethod.invoke(procObj, name);
			}else if (stdProcObj != null) {
				setLevMethod = stdProcObj.getClass().getMethod("setLev", int.class);
				setNameMethod = stdProcObj.getClass().getMethod("setName", String.class);
				
				setLevMethod.invoke(stdProcObj, (symTab != null && symTab.size() != 0 ) ? symTab.size()-1 :0);
				setNameMethod.invoke(stdProcObj, name);
			}else if (varObj != null) {
				setLevMethod = varObj.getClass().getMethod("setLev", int.class);
				setNameMethod = varObj.getClass().getMethod("setName", String.class);
				
				setLevMethod.invoke(varObj, (symTab != null && symTab.size() != 0 ) ? symTab.size()-1 :0);
				setNameMethod.invoke(varObj, name);
			}else if (refObj != null) {
				setLevMethod = refObj.getClass().getMethod("setLev", int.class);
				setNameMethod = refObj.getClass().getMethod("setName", String.class);
				
				setLevMethod.invoke(refObj, (symTab != null && symTab.size() != 0 ) ? symTab.size()-1 :0);
				setNameMethod.invoke(refObj, name);
			}
			if (top != null && !top.isEmpty()) {
				Method getNameMethod = null;
				for (Object obj : top) {
					
					getNameMethod = obj.getClass().getMethod("getName");
					String eName = (String) getNameMethod.invoke(obj);
					
					if (eName.equals(name)) {
						SC.mark("multiple definitions");
						return;
					}
				}
			}
		} catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			System.out.println("Error occured ... in ST.newDecl");
		}

		if (typeObj != null) {
			top.add(typeObj);
		}else if (constObj != null) {
			top.add(constObj);
		}else if (procObj != null) {
			top.add(procObj);
		}else if (stdProcObj != null) {
			top.add(stdProcObj);
		}else if (varObj != null) {
			top.add(varObj);
		}else if (refObj != null) {
			top.add(refObj);
		}
	}
	

	public static Object find(String name) {

		for (ArrayList<?> arrayList : symTab) {
//			Method getNameMethod = null;

			for (Object obj : arrayList) {
				try {
					Method getNameMethod = obj.getClass().getMethod("getName");
					String eName = (String) getNameMethod.invoke(obj);
					
					if (eName.equals(name)) 
						return obj;
					
				} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
					System.out.println("Error occured ... Security Exception");
				}
			}
		}
		SC.mark("undefined identifier " + name);
		Const constObj = new Const(null, 0);
		return constObj;
	}
	
	public static void openScope() {
		symTab.add(0, new ArrayList<Object>());
	}
	
	public static ArrayList<?> topScope(){
		if (symTab.size() == 0){
			return new ArrayList<Object>();
		}
		return symTab.get(0);
	}
	
	public static void closeScope() {
		symTab.remove(0);
	}
	
}

