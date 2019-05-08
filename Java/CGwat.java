import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CGwat {
	public static int curlev, memsize;
	public static ArrayList<String> asm;
	public static boolean createdGlobalVars = false;
	
	/*
	Function that initializes global vars. Creates the list that will store the generated code.
	*/
	public void genProgStart() {
		curlev = memsize = 0;
		asm = new ArrayList<String>( 
	            Arrays.asList("(module", 
	                          "(import \"P0lib\" \"write\" (func $write (param i32)))", 
	                          "(import \"P0lib\" \"writeln\" (func $writeln))",
	                          "(import \"P0lib\" \"read\" (func $read (result i32)))")); 
	}
	/*
	Following procedures "generate code" for all P0 types by determining the size of objects and store in the size field.

	Integers and booleans occupy 4 bytes
	The size of a record is the sum of the sizes of its field; the offset of a field is the sum of the size of the preceding fields
	The size of an array is its length times the size of the base type.
	*/
	public Object genBool(Object b) {
		Helper.setProperty(b, "setSize", 1);
		return b;
	}
	
	public Object genInt(Object i) {
		Helper.setProperty(i, "setSize", 4);
		return i;
	}
	
	public Object genRec(Object r){
	    var s = 0;
	    var temp = 0;
	    ArrayList <Object> fields = (ArrayList<Object>) Helper.getProperty(r, "getField");
	    
	    for (Object f : fields) {
	    	Helper.setProperty(f, "setOffset", s);
            String f_tp = (String) Helper.getProperty(f, "getTP");

            if (f_tp.equals("Int") || f_tp.equals("Bool") || f_tp.equals("Const")){
            	s = s + 1;
            }else {
            	String arr[] = f_tp.split(",");
            	s = s + arr.length;
            }
	    }
		Helper.setProperty(r, "setSize", s);

	    return r;
	}
	
	public Object genArray(Object a) {
		int a_length = (int) Helper.getProperty(a, "getLength");
		String a_base = (String) Helper.getProperty(a, "getBase");
		
		//TODO: here
		
		if (a_base.equals("Int") || a_base.equals("Bool")) {
			Helper.setProperty(a, "setSize", a_length * 1);
		}else if (a_base.startsWith("Record")){
			String s = a_base.substring(a_base.indexOf("["),a_base.length()-1);
			String arr [] = s.split("\\),");
			Helper.setProperty(a, "setSize", a_length * arr.length);
		}else if (a_base.startsWith("Array")){

		}
		
		return a;
	}
	/*
	The symbol table assigns to each entry the level of declaration in the field lev: int. Variables are assigned a name: str field by the symbol table and an adr: int field by the code generator. The use of the lev field is extended:

	lev > 0: local Int, Bool variable or parameter allocated in the procedure (function) call frame, accessed by name,
	lev = 0: global Int, Bool variable allocated as a WebAssembly global variable, accessed by name,
	lev = -1: Int, Bool variable allocated on the expression stack,
	lev = -2: Int, Bool, Array, Record variable allocated in the WebAssembly memory, accessed by adr.

	For each declared global variable, genGlobalVars(sc, start) allocates a global WebAssembly variable by the same name,
	if the type is Int or Bool, or reserves space in the memory, if the type is Array, Record. The parameter sc contains
	the top scope with all declarations parsed so far; only variable declarations from index start on in the top scope are considered.
	*/
	public void genGlobalVars(ArrayList <?> sc, int start) {
		
		for (int i=start; i<sc.size(); i++) {
			if (sc.get(i).getClass().getSimpleName().equals("Var")){
				try {
					Method getTPMethod = sc.get(i).getClass().getMethod("getTP");
					Method getNameMethod = sc.get(i).getClass().getMethod("getName");

					String sc_i_tp = (String) Helper.getProperty(sc.get(i), "getTP");
					String name = (String) getNameMethod.invoke(sc.get(i));

					
					if (sc_i_tp.equals("Int") || sc_i_tp.equals("Bool") ){
		                asm.add("(global $" + name + " (mut i32) i32.const 0)");
					}else if (sc_i_tp.startsWith("Array") || sc_i_tp.startsWith("Record")) {
		                Helper.setProperty(sc.get(i), "setLev", -2);
		                Helper.setProperty(sc.get(i), "setAdr", memsize);
		                //TODO: GET SIZE
//		                memsize = sc[i].tp.size;

		                int sc_i_tp_size = (int)Helper.getProperty(sc.get(i), "get_tp_length");

		                memsize = sc_i_tp_size;

		            }else {
						SC.mark("WASM: type?");
					}
				} catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		createdGlobalVars = true;
	}
	
	
	public Object genLocalVars(ArrayList <?> sc, int start) {
		
		for (int i=start; i<sc.size(); i++) {
			if (sc.get(i).getClass().getSimpleName().equals("Var")){
				try {
					Method getTPMethod = sc.get(i).getClass().getMethod("getTP");
					Method getNameMethod = sc.get(i).getClass().getMethod("getName");

					String sc_i_tp = (String) Helper.getProperty(sc.get(i), "getTP");
					String name = (String) getNameMethod.invoke(sc.get(i));

					
					if (sc_i_tp.equals("Int") || sc_i_tp.equals("Bool") ){
		                asm.add("(local $" + name + " i32)");
					}else if (sc_i_tp.startsWith("Array") || sc_i_tp.startsWith("Record")) {
						SC.mark("WASM: no local arrays, records");
		            }else {
						SC.mark("WASM: type?");
					}
				} catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}

	/*
	Procedure loadItem(x) generates code for loading x on the expression stack, assuming x is global Var,
	local Var, stack Var, memory Var, local Ref, stack Ref, Const.
	*/
	public void loadItem(Object x) {
		if (Helper.type(x).equals("Var")) {
			int x_lev = (int) Helper.getProperty(x, "getLev");
	    	String x_name = (String) Helper.getProperty(x, "getName");
			int x_adr = (int) Helper.getProperty(x, "getAdr");

			if (x_lev == 0)
				asm.add("global.get $" + x_name);
			else if (x_lev == curlev)
				asm.add("local.get $" + x_name);
			else if (x_lev == -2) {
				//TODO: add address, TEST THIS
	            asm.add("i32.const " + x_adr);
	            asm.add("i32.load");
			}else if (x_lev != -1)
				SC.mark("WASM: var level!");
		}else if (Helper.type(x).equals("Ref")) {
			int x_lev = (int) Helper.getProperty(x, "getLev");
	    	String x_name = (String) Helper.getProperty(x, "getName");
			if (x_lev == -1)
				asm.add("i32.load");
			else if (x_lev == curlev) {
	            asm.add("local.get $" + x_name);
	            asm.add("i32.load");
			}else
				SC.mark("WASM: ref level!");
		
		}else if (Helper.type(x).equals("Const")) {
	    	int x_val = (int) Helper.getProperty(x, "getVal");
			asm.add("i32.const " + String.valueOf(x_val));
		}
	}
	
	public Object genVar(Object x) {
		Object y = null;
    	int x_lev = (int) Helper.getProperty(x, "getLev");
    	Object x_tpObj =  Helper.getProperty(x, "getTPObj");
    	Object x_tp =  Helper.getProperty(x, "getTP");
    	String x_name = (String) Helper.getProperty(x, "getName");

		if (0 < x_lev && x_lev < curlev)
			SC.mark("WASM: level!");
		if (Helper.type(x).equals("Ref")) {
	        y = new ST.Ref((x_tpObj==null)? x_tp : x_tpObj);
	        
			Helper.setProperty(y, "setLev", x_lev); //y.lev = x.lev;
			Helper.setProperty(y, "setName", x_name); //y.name = x.name;
			
		}else if (Helper.type(x).equals("Var")) {
	        y = new ST.Var((x_tpObj==null)? x_tp : x_tpObj); 
	       
			Helper.setProperty(y, "setLev", x_lev); //y.lev = x.lev;
			Helper.setProperty(y, "setName", x_name); //y.name = x.name;
	        
	        if (x_lev == -2) {
	        	int x_adr = (int) Helper.getProperty(x, "getAdr");
				Helper.setProperty(y, "setAdr", x_adr);
//	        	y.adr = x.adr;
	        }
	    }
	    return y;
	}
	/*
	Procedure genConst(x) does not need to generate any code.
	*/
	public Object genConst(Object x) {
		return x;
	}
	/*
	Procedure genUnaryOp(op, x) generates code for op x if op is MINUS, NOT and x is Int, Bool, respectively.
	If op is AND, OR, item x is the first operand and an if instruction is generated.
	*/
	public Object genUnaryOp(int op, Object x) {
		loadItem(x);
	    if (op == SC.MINUS) {
	        asm.add("i32.const -1");
	        asm.add("i32.mul");
	        x = new ST.Var(ST.Int.class);
			Helper.setProperty(x, "setLev", -1);
	    }else if (op == SC.NOT){
	        asm.add("i32.eqz");
	        x = new ST.Var(ST.Bool.class); 
			Helper.setProperty(x, "setLev", -1);
	    }else if (op == SC.AND) {
	        asm.add("if (result i32)");
	        x = new ST.Var(ST.Bool.class);
			Helper.setProperty(x, "setLev", -1);
	    }else if (op == SC.OR){
	        asm.add("if (result i32)");
	        asm.add("i32.const 1");
	        asm.add("else");
	        x = new ST.Var(ST.Bool.class); 
			Helper.setProperty(x, "setLev", -1);

	    }else {
	    	SC.mark("WASM: unary operator?");
	    }
		return null;
	}
	/*
	Procedure genBinaryOp(op, x, y) generates code for x op y if op is PLUS, MINUS, TIMES, DIV, MOD. If op is AND, OR, code for x and the start of an if
	instruction has already been generated; code for y and the remainder of the if instruction is generated.
	*/
	public Object genBinaryOp(int op, Object x, Object y) {
		Set<Integer> ARITHMETIC = Set.of(SC.PLUS, SC.MINUS, SC.TIMES, SC.DIV, SC.MOD);
		if (ARITHMETIC.contains(op)){
	        loadItem(x); loadItem(y);
	        if (op == SC.PLUS)
	            asm.add("i32.add");
	        else if (op == SC.MINUS)
	            asm.add("i32.sub");
	        else if (op == SC.TIMES)
	            asm.add("i32.mul");
	        else if (op == SC.DIV)
	            asm.add("i32.div_s");
	        else if (op == SC.MOD)
	            asm.add("i32.rem_s");
	        else
	            asm.add("?");
	        
	        x = new ST.Var(ST.Int.class); 
			Helper.setProperty(x, "setLev", -1);
	    }else if (op == SC.AND){
	        loadItem(y);
	        asm.add("else");
	        asm.add("i32.const 0");
	        asm.add("end");
	        x = new ST.Var(ST.Bool.class); 
			Helper.setProperty(x, "setLev", -1); 
	    }else if (op == SC.OR){
	        loadItem(y);
	        asm.add("end");
	        x = new ST.Var(ST.Bool.class); 
			Helper.setProperty(x, "setLev", -1);
	    }else {
	        assert(false);
	    }
	    return x;
	}
	
	/*
	Procedure `genRelation(op, x, y)` generates code for `x op y` if `op` is `EQ`, `NE`, `LT`, `LE`, `GT`, `GE`.
	*/
	public Object genRelation(int op, Object x, Object y) {
		loadItem(x);
		loadItem(y);

		if (op == SC.EQ) {
			asm.add("i32.eq");
		}else if (op == SC.NE) {
			asm.add("i32.ne");
		}else if (op == SC.LT) {
			asm.add("i32.lt_s");
		}else if (op == SC.GT) {
			asm.add("i32.gt_s");
		}else if (op == SC.LE) {
			asm.add("i32.le_s");
		}else if (op == SC.GE) {
			asm.add("i32.ge_s");
		}
		x = new ST.Var(ST.Bool.class);
		Helper.setProperty(x, "setLev", -1);
		return x;
	}
	/*
	Procedure genSelect(x, f) generates code for x.f, provided f is in x.fields. If x is Var, i.e. allocated in memory, only x.adr is updated and no code is generated.
	If x is Ref, i.e. a reference to memory, code for adding the offset of f is generated. An updated item is returned.
	*/
	public Object genSelect(Object x, Object f) {
		int x_lev = (int) Helper.getProperty(x, "getLev");
		int x_adr = (int) Helper.getProperty(x, "getAdr");
		String x_name = (String) Helper.getProperty(x, "getName");

		//TODO: fix hard-coding
		int f_offset = 0;//int f_offset = (int) Helper.getProperty(f, "getOffset");

		if (Helper.type(x).equals("Var")) {
			x_adr = x_adr + f_offset;
			Helper.setProperty(x, "setAdr", x_adr);
	    }else if (Helper.type(x).equals("Ref")){
	        if (x_lev > 0) {
	            asm.add("local.get $" + x_name);
	        }
	        asm.add("i32.const " + f_offset);
	        asm.add("i32.add");

			Helper.setProperty(x, "setLev", -1); 		//x.lev = -1;
	    }
		String f_tp = f.toString().replaceAll("\\)|\\]", "").substring(f.toString().indexOf("tp")+5);
	    
		Helper.setProperty(x, "setTP", f_tp); //x.tp = f_tp;

	    return x;
	}
	/*
	Procedure genIndex(x, y) generates code for x[y], assuming x is Var or Ref, x.tp is Array, and y.tp is Int.
	If y is Const, only x.adr is updated and no code is generated, otherwise code for array index calculation is generated.
	*/
	public Object genIndex(Object x, Object y) {
		int x_lev = (int) Helper.getProperty(x, "getLev");
		String x_tp_base =  (String) Helper.getProperty(x, "get_tp_base");
		int x_adr = (int) Helper.getProperty(x, "getAdr");
		int x_tp_lower = (int) Helper.getProperty(x, "get_tp_lower");
		int x_tp_base_size = (int) Helper.getProperty(x, "get_tp_base_size");


		if (Helper.type(x).equals("Var")){
	        if (Helper.type(y).equals("Const")) {
        		int y_val = (int) Helper.getProperty(y, "getVal");
	        	//TODO TEST THIS
	            if (x_tp_base.equals("Int") || x_tp_base.equals("Bool")) {
	                x_adr += (y_val - x_tp_lower) * 1;
	            }else {
	                x_adr += (y_val - x_tp_lower) * x_tp_base_size;
	            }
//	            x.tp = x.tp.base;
	            Helper.setProperty(x, "setTPObj", x_tp_base);
	        }else{
	            loadItem(y);
	            if (x_tp_lower != 0){
	                asm.add("i32.const " + x_tp_lower);
	                asm.add("i32.sub");
	            }
	            if (x_tp_base.toString().equals("Int") || x_tp_base.toString().equals("Bool"))
	            	asm.add("i32.const " + 1);
	            else
	            	asm.add("i32.const " + x_tp_base_size);
	            
	            asm.add("i32.mul");
	            asm.add("i32.const " + x_adr);
	            asm.add("i32.add");
	            x = new ST.Ref(x_tp_base); 
	            Helper.setProperty(x, "setLev", -1);	//x.lev = -1;

	        }
	    }else{
	        if (x_lev == curlev) {
	            loadItem(x);
	            //x.lev = -1;
	            Helper.setProperty(x, "setLev", -1);	//x.lev = -1;
	        }
	        if (Helper.type(y).equals("Const")) {
	        	int y_val = (int) Helper.getProperty(y, "getVal");
	            if (x_tp_base.equals("Int") || x_tp_base.equals("Bool")) {
	                asm.add("i32.const " + (y_val - x_tp_lower) * 1);
	            }else {
	                asm.add("i32.const " + (y_val - x_tp_lower) * x_tp_base_size);
	            }
	            asm.add("i32.add");
	        }else {
	            loadItem(y);
	            asm.add("i32.const " + x_tp_lower);
	            asm.add("i32.sub");
	            if (x_tp_base.equals("Int") || x_tp_base.equals("Bool"))
	                asm.add("i32.const 1");
	            else
	                asm.add("i32.const " + x_tp_base_size);
	            asm.add("i32.mul");
	            asm.add("i32.add");
	        }
//	        x.tp = x.tp.base;
            Helper.setProperty(x, "setTPObj", x_tp_base);
	    }
	    return x;
	}
	
	/*
	Procedure `genAssign(x, y)` generates code for `x := y`, provided `x` is `Var`, `Ref` and `y` is `Var`, `Ref`.
	*/
	public Object genAssign(Object x, Object y) {
		int x_lev = (int) Helper.getProperty(x, "getLev");
    	String x_name = (String) Helper.getProperty(x, "getName");

		if (Helper.type(x).equals("Var")) {
			if (x_lev == -2) 
				asm.add("i32.const ");
			
			loadItem(y);
			if (x_lev == 0)
				asm.add("global.set $" + x_name);
			else if (x_lev == curlev) {
				asm.add("local.set $" + x_name);
			}else if (x_lev == -2) {
				asm.add("i32.store");
			}else if (x_lev != -1)
				SC.mark("WASM: level!");
		}else if (Helper.type(x).equals("Ref")) {
			if (x_lev == curlev)
				asm.add("local.get $" + x_name);
			loadItem(y);
			asm.add("i32.store");
		}
		
		return null;
	}

	
	public void genProgEntry(String ident) {
		asm.add("(func $program");
	}
	
	public String genProgExit(Object x) {
		int val  = (int)Math.floor(memsize / Math.pow(2,16) + 1);
	    asm.add(")\n(memory " + val + ")\n(start $program)\n)");
	    
	    String result = "";
	    for (String string : asm) {
			result += string + "\n";
		}
	    return result;
	}

	/*

	*/
	public Object genProcStart(String ident, ArrayList<?> fp){
	    if (curlev > 0)
	    	SC.mark("WASM: no nested procedures");
	    curlev = curlev + 1;

	    String str = "";

	    for (Object e : fp){
	    	String e_name = (String) Helper.getProperty(e, "getName");
	        str += "(param $" + e_name +" i32)";
	    }
	    asm.add("(func $" + ident + " " + str);


	    for (Object p : fp) {
	    	String p_tp = (String) Helper.getProperty(p, "getTP");
	        if ((p_tp.equals("Int") || p_tp.equals("Bool")) && Helper.type(p).equals("Ref"))
	            SC.mark("WASM: only array and record reference parameters");
	        else if ((Helper.type(p_tp).equals("Array") || Helper.type(p_tp).equals("Record")) && Helper.type(p).equals("Var"))
	            SC.mark("WASM: no structured value parameters");
	    }
	    
	    return null;
	}
	
	public void genProcEntry(String indent, Object parsize, Object localsize) {
		//pass
	}
	
	/*

	*/
	public void genProcExit(Object x, Object parsize, Object localsize) {
	    curlev = curlev - 1;
	    asm.add(")");
	}
	/*

	*/
	public Object genActualPara(Object ap, Object fp, int n) {
		if (Helper.type(fp).equals("Ref")) {
			int ap_lev = (int) Helper.getProperty(ap, "getLev");
	        if (ap_lev == -2) {
	        	int ap_adr = (int) Helper.getProperty(ap, "getAdr");
	        	asm.add("i32.const " + ap_adr);
	        }
	    }else if (Helper.type(ap).equals("Var") || Helper.type(ap).equals("Ref") || Helper.type(ap).equals("Const")){
	        loadItem(ap);
	    }else {
	        SC.mark("unsupported parameter type");
	    }
	    return null;
	}
	/*

	*/
	public Object genCall(Object pr, ArrayList<Object> ap) {
		String pr_name = (String) Helper.getProperty(pr, "getName");
		asm.add("call $" + pr_name);
		return null;
	}
	/*

	*/
	public Object genRead(Object x) {
		asm.add("call $read");
		Object y = new ST.Var(ST.Int.class);
		Helper.setProperty(y, "setLev", -1);
		return null;
	}
	/*

	*/
	public Object genWrite(Object x) {
		loadItem(x);
		asm.add("call $write");
		return null;
	}

	public Object genWriteln() {
		asm.add("call $writeln");
		return null;
	}

	public Object genSeq(Object x, Object y) {
		return null;
	}

	public Object genThen(Object x) {
	    loadItem(x);
	    asm.add("if");
	    return x;
	}
	public Object genIfThen(Object x, Object y) {
	    asm.add("end");
		return null;
	}
	
	public Object genElse(Object x, Object y) {
	    asm.add("else");
		return null;
	}
	
	public Object genIfElse(Object x, Object y, Object z) {
	    asm.add("end");
		return null;
	}

	public Object genWhile() {
	    asm.add("loop");
		return null;
	}
	
	public Object genDo(Object x) {
	    loadItem(x);
	    asm.add("if");
	    return x;
	}

	public Object genWhileDo(Object t, Object x, Object y) {
	    asm.add("br 1");
	    asm.add("end");
	    asm.add("end");
		return null;
	}

}