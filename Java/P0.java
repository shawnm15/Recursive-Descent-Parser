import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class P0 {
	
	/*
	The below declarations are the first and follows sets for resucrive descent parsing that will used throughout
	many of the functions below.
	*/
	public static Set<Integer> FIRSTFACTOR = Set.of(SC.IDENT, SC.NUMBER, SC.LPAREN, SC.NOT);
	public static Set<Integer> FOLLOWFACTOR = Set.of(SC.TIMES, SC.DIV, SC.MOD, SC.AND, SC.OR, SC.PLUS, 
													SC.MINUS, SC.EQ, SC.NE, SC.LT, SC.LE, SC.GT, 
													SC.GE, SC.COMMA, SC.SEMICOLON, SC.THEN, SC.ELSE, 
													SC.RPAREN, SC.RBRAK, SC.DO, SC.PERIOD,SC.END);
	public static Set<Integer> FIRSTEXPRESSION = Set.of(SC.PLUS, SC.MINUS, SC.IDENT, SC.NUMBER, SC.LPAREN, SC.NOT);
	public static Set<Integer> FIRSTSTATEMENT = Set.of(SC.IDENT, SC.IF, SC.WHILE, SC.BEGIN);
	public static Set<Integer> FOLLOWSTATEMENT = Set.of(SC.SEMICOLON, SC.END, SC.ELSE);
	public static Set<Integer> FIRSTTYPE = Set.of(SC.IDENT, SC.RECORD, SC.ARRAY, SC.LPAREN);
	public static Set<Integer> FOLLOWTYPE = Set.of(SC.SEMICOLON);
	public static Set<Integer> FIRSTDECL = Set.of(SC.CONST, SC.TYPE, SC.VAR, SC.PROCEDURE);
	public static Set<Integer> FOLLOWDECL = Set.of(SC.BEGIN);
	public static Set<Integer> FOLLOWPROCCALL = Set.of(SC.SEMICOLON, SC.END, SC.ELSE);
	public static Set<Integer> STRONGSYMS = Set.of(SC.CONST, SC.TYPE, SC.VAR, SC.PROCEDURE, SC.WHILE, SC.IF, SC.BEGIN, SC.EOF);

	public static CGwat CG = new CGwat();
	
	public static int start;
	
	/*
	This functions helps understand and generate code for selector expressions. A selector looks like
	x[i] or x.f. The passed parameter of x is the identifier in front of the selector. Depending on the type of
	the selector, the appropriate code is generated in the CGWat.
	 */
	private static Object selector(Object x) {
		while (SC.sym == SC.PERIOD || SC.sym == SC.LBRAK ) {
        	String x_tp = (String) Helper.getProperty(x, "getTP");
	        if (SC.sym == SC.PERIOD) {
	            SC.getSym();
	            if (SC.sym == SC.IDENT) {
                    String x_tp_base = (String) Helper.getProperty(x, "get_tp_base");
	                if (x_tp_base.startsWith("Record")) {
	                    boolean found = false;
	                    String [] x_tp_fields = Helper.getFieldProp(x_tp);
	                    for (String f : x_tp_fields) {
	                    	String f_name = f.substring(f.indexOf("name = ")+7, f.indexOf(","));
	                        if (f_name.equals(SC.val)) {
	                            found = true;
	                            x = CG.genSelect(x, f);
	                            break;
	                        }
	                    }
	                    if (!found)
	                        SC.mark("not a field");
	                    SC.getSym();
	                }else {
	                    SC.mark("not a record");
	                }
	            }else {
	                SC.mark("identifier expected");
	            }
	        }else {
	            SC.getSym(); Object y = expression();
	        	String y_tp = (String) Helper.getProperty(y, "getTP");
	            if (x_tp.startsWith("Array")) {
	                if (y_tp.equals("Int")) {
	                	try {
	                		Method getMethod = x.getClass().getMethod("getVal");
	        				int y_val = (int) getMethod.invoke(x);
		                	int x_tp_lower = (int) Helper.getProperty(x, "get_tp_lower");
		                	int x_tp_length = (int) Helper.getProperty(x, "get_tp_length");

		                	if (Helper.type(y).equals("Const") && (y_val < x_tp_lower || y_val >= x_tp_lower + x_tp_length))
		                        SC.mark("index out of bounds");
	                	}catch(NoSuchMethodException |SecurityException  | IllegalArgumentException | IllegalAccessException | InvocationTargetException  e ) {
		                     x = CG.genIndex(x, y);
	                	}
	                	
	                }else {
	                	SC.mark("index not integer");
	                }
	            }else { 
	            	SC.mark("not an array");
	            }

	            if (SC.sym == SC.RBRAK)
	                SC.getSym();
	            else
	                SC.mark("] expected");
	        }
	    }
	    return x;
	}

	/*
	This function generates code for expressions that are factors. So a factor represented
	as a grammar is 'factor ::= ident selector | integer | "(" expression ")" | "not" factor.
	The function below accounts for each case of the grammar.
	 */
	private static Object factor() {
		Object x = null;
		if (!FIRSTFACTOR.contains(SC.sym)) {
			SC.mark("expression expected");
		    while (!FIRSTFACTOR.contains(SC.sym) && !FOLLOWFACTOR.contains(SC.sym) && !STRONGSYMS.contains(SC.sym)) {
		    	SC.getSym();
		    }
		}
		if (SC.sym == SC.IDENT) {
			x = ST.find(SC.val);
			if (Helper.type(x).equals("Var") || Helper.type(x).equals("Ref")) {
				x = CG.genVar(x); SC.getSym();
			}else if (Helper.type(x).equals("Const")) {
				int x_val = (int) Helper.getProperty(x, "getVal");
				String x_tp = (String) Helper.getProperty(x, "getTP");
				if (x_tp.equals("Int")) {
					x = new ST.Const(ST.Int.class, x_val);
				}else if (x_tp.equals("Bool")) {
					x = new ST.Const(ST.Bool.class, x_val);
				}
				x = CG.genConst(x); SC.getSym();
			}else {
				SC.mark("expression expected");
			}
			x = selector(x);
		}else if (SC.sym == SC.NUMBER) {
			//TODO check if Integer.parseInt(SC.val) is a bug
			x = new ST.Const(ST.Int.class, Integer.parseInt(SC.val)); x = CG.genConst(x); SC.getSym();
		}else if (SC.sym == SC.LPAREN) {
			SC.getSym(); x = expression();
			if (SC.sym == SC.RPAREN)
				SC.getSym();
			else
				SC.mark(") expected");
		}else if (SC.sym == SC.NOT){
			SC.getSym(); x = factor();
			
        	String x_tp = (String) Helper.getProperty(x, "getTP");

	        if (!x_tp.equals("Bool")) {
	        	SC.mark("not boolean");
	        }else if (Helper.type(x).equals("Const")) {
            	int x_val = (int) Helper.getProperty(x, "getVal"); 
				Helper.setProperty(x, "setVal", 1 - x_val); //x.val = 1 - x.val; 
	        }else
	        	x = CG.genUnaryOp(SC.NOT, x);
		}else {
			 x = new ST.Const(null, 0);
		}
		
		return x;
	}
	
	/*
	The term function parses grammar in the form 'term ::= factor {("*" | "div" | "mod" | "and") factor}'
	It will generate code for the term. If the term is a constant, then we call a Const item is returned
	(and code may not need to be generated).
	If the term is not a constant, the location of the result is returned as determined by the code generator.'
	 */
	private static Object term() {
		Object x = factor();
		Object y = null;
		while (SC.sym == SC.TIMES || SC.sym == SC.DIV || SC.sym == SC.MOD || SC.sym == SC.AND) {
			int op = SC.sym; SC.getSym();
	        if (op == SC.AND && !Helper.type(x).equals("Const")) 
	        	x = CG.genUnaryOp(SC.AND, x);
	        y = factor();
	        
        	String x_tp = (String) Helper.getProperty(x, "getTP");
        	String y_tp = (String) Helper.getProperty(y, "getTP");
	        
        	if (x_tp.equals("Int") && y_tp.equals("Int") && (op == SC.TIMES || op == SC.DIV || op == SC.MOD)) {
	            if (Helper.type(x).equals("Const") && Helper.type(y).equals("Const")) {
	            	int x_val = (int) Helper.getProperty(x, "getVal");
    				int y_val = (int) Helper.getProperty(y, "getVal");
	                if (op == SC.TIMES) {
	    				Helper.setProperty(x, "setVal", x_val * y_val); //x.val = x.val * y.val;
	                }else if (op == SC.DIV) {
	                	int divide = (int)(x_val / y_val);
	    				Helper.setProperty(x, "setVal", divide); //x.val = x.val / y.val;
	                }else if (op == SC.MOD)
	    				Helper.setProperty(x, "setVal", x_val % y_val); //x.val = x.val % y.val;
	            }else {
	            	x = CG.genBinaryOp(op, x, y);
	            }
	        }else if (x_tp.equals("Bool") && y_tp.equals("Bool") && op == SC.AND) {
	        	Object x_val = Helper.getProperty(x, "getVal");
	        	if (Helper.type(x).equals("Const")) {
	        		if (x_val instanceof Integer)
	        			if (x_val.equals(1))
	        				x = y;
	        		
//	                if (x_val.equals("true") || x_val.equals("1"))
//	                    x = y;
	                
	        	}else {
	            	x = CG.genBinaryOp(SC.AND, x, y);
	        	}
	        }else {
	        	SC.mark("bad type");
	        }
		}
        return x;
	}
	
	/*
	This functions understands productions in the form of 'simpleExpression ::= ["+" | "-"] term {("+" | "-" | "or") term}'.
	Code is generated in for the simple expression if no error is reported. If the simple expression is a constant,
	a Const item is returned (and code may not need to be generated). Ihe the simple expression being parsed is not constant,
	then the location of the result is returned as determined by the code generator module.
	*/
	private static Object simpleExpression() {
		Object x = null;
		Object y = null;
		if (SC.sym == SC.PLUS) {
			SC.getSym();
			x = term();
		}else if (SC.sym == SC.MINUS) {
			SC.getSym();
			x = term();
        	String x_tp = (String) Helper.getProperty(x, "getTP");

			if (!x_tp.equals("Int")) {
				SC.mark("bad type");
			}else if (Helper.type(x).equals("Const")) {
				int x_val = (int) Helper.getProperty(x, "getVal");					
				Helper.setProperty(x, "setVal", -x_val);
			}else {
				x = CG.genUnaryOp(SC.MINUS, x);
			}
		}else {
			x = term();
		}
		
		while (SC.sym == SC.PLUS || SC.sym == SC.MINUS || SC.sym == SC.OR ) {
			int op = SC.sym; SC.getSym();
			if (op == SC.OR && !Helper.type(x).equals("Const")) {
				x = CG.genUnaryOp(SC.OR, x);
			}
			y = term();
			
        	String x_tp = (String) Helper.getProperty(x, "getTP");
        	String y_tp = (String) Helper.getProperty(y, "getTP");

			if (x_tp.equals("Int") && y_tp.equals("Int") && (op == SC.PLUS || op == SC.MINUS)) {
	            if (Helper.type(x).equals("Const") && Helper.type(y).equals("Const")) {
	            	int x_val = (int) Helper.getProperty(x, "getVal");
        			int y_val = (int) Helper.getProperty(y, "getVal");
	                if (op == SC.PLUS) {
	    				Helper.setProperty(x, "setVal", x_val+y_val);	//x.val = x.val + y.val;
	                }else if (op == SC.MINUS) {
	                	Helper.setProperty(x, "setVal", x_val-y_val); //x.val = x.val - y.val;
	                }
	            }else
	            	x = CG.genBinaryOp(op, x, y);
			}else if (x_tp.equals("Bool") && y_tp.equals("Bool") && op == SC.OR) {
				if (Helper.type(x).equals("Const")) {
					Object x_val = Helper.getProperty(x, "getVal");
//        			if (!x_val.equals("true") || !x_val.equals("1")) 
//	                    x = y;
	        		if (x_val instanceof Integer)
	        			if (!x_val.equals(1))
	        				x = y;
				}else
					x = CG.genBinaryOp(SC.OR, x, y);
			}else 
				SC.mark("bad type");
			
		}
		return x;
	}
	
	/*
	The function below handles expression with the grammar production of 'expression ::= simpleExpression {("=" | "<>" | "<" | "<=" | ">" | ">=") simpleExpression}'
	Code is generated via CGwat if no error is reported. The location of the result is returned as determined by the code generator.
	*/
	public static Object expression() {
		Object x = simpleExpression();
		Set<Integer> symbols = Set.of(SC.EQ, SC.NE, SC.LT, SC.LE, SC.GT, SC.GE);

		while (symbols.contains(SC.sym)) {
	        int op = SC.sym;
			SC.getSym();
	        Object y = simpleExpression();
	        
        	String x_tp = (String) Helper.getProperty(x, "getTP");
        	String y_tp = (String) Helper.getProperty(y, "getTP");
	        
        	//TODO: test if this works
        	if (x_tp.equals(y_tp) && (y_tp.equals("Int") || y_tp.equals("Bool"))) {
	            if (Helper.type(x).equals("Const") && Helper.type(y).equals("Const")) {
        			String x_val = (String) Helper.getProperty(x, "getVal");
        			String y_val = (String) Helper.getProperty(y, "getVal");
        			int x_int = -1;
        			int y_int = -1;
        			if (x_tp.equals("Bool")) {
        				if (x_val.equals("true"))
        					x_int = 1;
        				else if (x_val.equals("false"))
        					x_int = 0;
        				else if (y_val.equals("true"))
        					y_int = 1;
        				else if (y_val.equals("false"))
        					y_int = 0;
        			}
	            	if (op == SC.EQ) {
            			//x.val = x.val == y.val;
	            		if (x_tp.equals("Int"))	            			
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toInt(x_val)==Helper.toInt(y_val)));
	            		else	            			
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toBool(x_val)==Helper.toBool(y_val)));
	                }else if (op == SC.NE) {
	                	//x.val = x.val != y.val;
	                	if (x_tp.equals("Int"))            			
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toInt(x_val)!=Helper.toInt(y_val)));
	            		else
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toBool(x_val)!=Helper.toBool(y_val)));
	                }else if (op == SC.LT) {
	                	//x.val = x.val < y.val;
	                	if (x_tp.equals("Int"))	            			
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toInt(x_val)<Helper.toInt(y_val)));
	            		else
		            		Helper.setProperty(x, "setVal", Helper.toStr(x_int < y_int));
	                }else if (op == SC.LE) {
	                	//x.val = x.val <= y.val;
	                	if (x_tp.equals("Int"))	            			
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toInt(x_val)<=Helper.toInt(y_val)));
	            		else
		            		Helper.setProperty(x, "setVal", Helper.toStr(x_int <= y_int));
	                }else if (op == SC.GT) {
	                	//x.val = x.val > y.val;
	                	if (x_tp.equals("Int"))	            			
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toInt(x_val)>Helper.toInt(y_val)));
	            		else
		            		Helper.setProperty(x, "setVal", Helper.toStr(x_int > y_int));
	                }else if (op == SC.GE) {
	                	//x.val = x.val >= y.val;
	                	if (x_tp.equals("Int"))	            			
		            		Helper.setProperty(x, "setVal", Helper.toStr(Helper.toInt(x_val)>=Helper.toInt(y_val)));
	            		else
		            		Helper.setProperty(x, "setVal", Helper.toStr(x_int >= y_int));
	                }
//	                x.tp = Bool;
	                Helper.setProperty(x, "setTP", "Bool");
		        } else {
		        	x = CG.genRelation(op, x, y);
		        }
	       
	   
			}else {
	        	SC.mark("bad type");
	        }
		}
		return x;
	}

	/*
	The function below parses the production 'compoundStatement ::= "begin" statement {";" statement} "end".
	The respective code is generated for the term if no error is reported. A result is returned as determined by the code generator.
	*/
	private static Object compoundStatement() {
		if (SC.sym == SC.BEGIN) {
			SC.getSym();
		}else {
			SC.mark("'begin' expected");
		}
		Object x = statement();
		while (SC.sym == SC.SEMICOLON || FIRSTSTATEMENT.contains(SC.sym)) {
			if (SC.sym == SC.SEMICOLON)
				SC.getSym();
			else
				SC.mark("; missing");
			
			Object y = statement();
			x = CG.genSeq(x, y);
		}
		if (SC.sym == SC.END) {
			SC.getSym();
		}else {
			SC.mark("'end' expected");
		}
		return x;
	}
	

	/*
	The function below is quite heavy. It parses the production:
	statement ::= ident selector ":=" expression |
	              ident "(" [expression {"," expression}] ")" |
	              compoundStatement |
	              "if" expression "then" statement ["else"statement] |
	              "while" expression "do" statement.
	and generates code for the statement if no error is reported. A result is returned as determined by the code generator.
	*/
	private static Object statement() {
		if (!FIRSTSTATEMENT.contains(SC.sym)) {
			SC.mark("statement expected"); SC.getSym();
			while (!FIRSTSTATEMENT.contains(SC.sym) && !FOLLOWSTATEMENT.contains(SC.sym) && !STRONGSYMS.contains(SC.sym)) {
				SC.getSym();
		    }
		}
		Object x = null;
		Object y = null;
		if (SC.sym == SC.IDENT) {
			x = ST.find(SC.val); SC.getSym();
            if (Helper.type(x).equals("Var") || Helper.type(x).equals("Ref")) {
            	x = CG.genVar(x); 
            	x = selector(x);
                if (SC.sym == SC.BECOMES) {
                	SC.getSym(); 
                	y = expression();
                	
                	String x_tp = (String) Helper.getProperty(x, "getTP");
                	String y_tp = (String) Helper.getProperty(y, "getTP");
                	
              
                    if (x_tp.equals(y_tp) && (y_tp.equals("Int") || y_tp.equals("Bool")))
                    	x = CG.genAssign(x, y);
                    else
                    	SC.mark("incompatible assignment");

                }else if (SC.sym == SC.EQ) {
                	SC.mark(":= expected");
                	SC.getSym(); 
                	y = expression();
                }else
                	SC.mark(":= expected");
            }else if (Helper.type(x).equals("Proc") || Helper.type(x).equals("StdProc")) {
            	List<Object> fp =(ArrayList<Object>)Helper.getProperty(x, "getPar");
            	ArrayList <Object> ap = new ArrayList<>();
            	int i = 0;
            	
            	if (SC.sym == SC.LPAREN) {
            		SC.getSym();
            		if (FIRSTEXPRESSION.contains(SC.sym)) {
            			y = expression();
            			Object fp_i = fp.get(0);
            			//TODO add more cases 
//            			String fp_i_tp = Helper.type(fp_i);
            			String fp_i_str = fp_i.toString();

            
            			if (fp_i_str.substring(1,fp_i_str.indexOf("(")).equals("Var")   ){
            				if ((fp_i_str.substring(fp_i_str.indexOf("(")+1,fp_i_str.indexOf(")")).equals("Int")))
            					fp_i = new ST.Var(ST.Int.class);
            				else if ((fp_i_str.substring(fp_i_str.indexOf("(")+1,fp_i_str.indexOf(")")).equals("Bool")))
            					fp_i = new ST.Var(ST.Bool.class);
            			}else if (fp_i_str.substring(1, fp_i_str.indexOf("(")).equals("Ref")){
            				if (fp_i_str.substring(fp_i_str.indexOf("(")+1,fp_i_str.indexOf(")")).equals("Int"))
            					fp_i = new ST.Ref(ST.Int.class);
            				else if ((fp_i_str.substring(fp_i_str.indexOf("(")+1,fp_i_str.indexOf(")")).equals("Bool")))
            					fp_i = new ST.Ref(ST.Bool.class);
            			}

            			String f_i_tp = (String) Helper.getProperty(fp_i, "getTP");
            			String y_tp = (String) Helper.getProperty(y, "getTP");

                        if (i < fp.size() && !(fp.size() == 1 && "".equals(fp.get(0)))) {
                            if ((Helper.type(fp_i).equals("Var") || Helper.type(y).equals("Var")) && f_i_tp.equals(y_tp)) {
                                if (Helper.type(x).equals("Proc")) {
                                    ap.add(CG.genActualPara(y, fp_i, i));
                                }
                            }else {
                            	SC.mark("illegal parameter mode");
                            }
                        }else {
                        	SC.mark("extra parameter");
                        }
                        i = i + 1;
                        
                        while (SC.sym == SC.COMMA) {
                        	SC.getSym();
                            y = expression();
                            if (i < fp.size() && !(fp.size() == 1 && "".equals(fp.get(0)))) {
                                if ((Helper.type(fp_i).equals("Var") || Helper.type(y).equals("Var")) && f_i_tp.equals(y_tp)) {
                                    if (Helper.type(x).equals("Proc")) {
                                        ap.add(CG.genActualPara(y, fp_i, i));
                                    }
                                }else {
                                	SC.mark("illegal parameter mode");
                                }
                            }else {
                            	SC.mark("extra parameter");
                            }
                            i = i + 1;
                        }
 
            		}
            		
            		if (SC.sym == SC.RPAREN)
            			SC.getSym();
            		else
            			SC.mark("')' expected");
            	}
            	if (i < fp.size() && !(fp.size() == 1 && "".equals(fp.get(0)))) {
            		SC.mark("too few parameters");
            	}else if (Helper.type(x).equals("StdProc")) {
            		String x_name = (String) Helper.getProperty(x, "getName");
            		if (x_name.equals("read"))
            			x = CG.genRead(y);
            		else if  (x_name.equals("write"))
            			x = CG.genWrite(y);
            		else if  (x_name.equals("writeln"))
            			x = CG.genWriteln();
            	}else {
            		x = CG.genCall(x, ap);
            	}
            }
            else {
            	SC.mark("variable or procedure expected");
            }
			
		}else if (SC.sym == SC.BEGIN) {
			x = compoundStatement();
		}else if (SC.sym == SC.IF){
			SC.getSym();
			x = expression();
			
        	String x_tp = (String) Helper.getProperty(x, "getTP");

			if (x_tp.equals("Bool"))
				x = CG.genThen(x);
			else
				SC.mark("boolean expected");
			
			if (SC.sym == SC.THEN)
				SC.getSym();
			else
				SC.mark("'then' expected");
			
			y = statement();
			
			if (SC.sym == SC.ELSE) {
				if (x_tp.equals("Bool"))
			    	y = CG.genElse(x, y);
			    SC.getSym();
			    Object z = statement();
			    if (x_tp.equals("Bool"))
			    	x = CG.genIfElse(x, y, z);
			}else {
			    if (x_tp.equals("Bool"))
			    	x = CG.genIfThen(x, y);
			}
		}else if (SC.sym == SC.WHILE) {
			SC.getSym();
			Object t = CG.genWhile();
			x = expression();
        	String x_tp = (String) Helper.getProperty(x, "getTP");

			if (x_tp.equals("Bool"))
				x = CG.genDo(x);
	        else
	        	SC.mark("boolean expected");
			
	        if (SC.sym == SC.DO)
	        	SC.getSym();
	        else
	        	SC.mark("'do' expected");
	        y = statement();
	        if (x_tp.equals("Bool"))
	        	x = CG.genWhileDo(t, x, y);
			
		}else {
			x = null;
		}
		return x;
	}
	
	/*
	The function typ() parses the production

	type ::= ident |
	         "array" "[" expression ".." expression "]" "of" type |
	         "record" typedIds {";" typedIds} "end"

	This function returns a type descriptor if no error is reported by the program. The array bound are checked to be constants;
	the lower bound must be smaller or equal to the upper bound.
	*/
	public static Object typ() {
		if (!FIRSTTYPE.contains(SC.sym)) {
			SC.mark("type expected"); 
		    while (!FIRSTTYPE.contains(SC.sym) && !FOLLOWTYPE.contains(SC.sym) && !STRONGSYMS.contains(SC.sym)) {
		    	SC.getSym();
		    }
		}
		Object x = null;
		
		if (SC.sym == SC.IDENT) {
			String ident = SC.val;
			x = ST.find(ident);
			SC.getSym();
			
			
			if (Helper.type(x).equals("Type")) {
				
				String x_val = (String) Helper.getProperty(x, "getVal");
				//TODO add more cases for other then Int and Bool
				if (x_val.equals("Int")) {
					x = new ST.Type(ST.Int.class);
				}else if(x_val.equals("Bool")) {
					x = new ST.Type(ST.Bool.class);
				}
			}else {
				SC.mark("Not a type");
				x = new ST.Type((String)null);
			}
		}else if (SC.sym == SC.ARRAY){
	        SC.getSym();
	        if (SC.sym == SC.LBRAK)
	            SC.getSym();
	        else
	            SC.mark("'[' expected");
	        x = expression();
	        if (SC.sym == SC.PERIOD)
	            SC.getSym();
	        else SC.mark("'.' expected");
	        if (SC.sym == SC.PERIOD)
	            SC.getSym();
	        else SC.mark("'.' expected");
	        Object y = expression();
	        if (SC.sym == SC.RBRAK)
	            SC.getSym();
	        else SC.mark("']' expected");
	        if (SC.sym == SC.OF)
	            SC.getSym();
	        else
	            SC.mark("'of' expected");
	        
	        Object zObj = typ();
			Object z = (Object) Helper.getProperty(zObj, "getVal");

			int x_val = (int) Helper.getProperty(x, "getVal");
			int y_val = (int) Helper.getProperty(y, "getVal");

	        if (!Helper.type(x).equals("Const") || x_val < 0) {
	            SC.mark("bad lower bound");
	            x = new ST.Type((String)null);
	        }else if (!Helper.type(y).equals("Const") || y_val < x_val) {
	            SC.mark("bad upper bound");
	            x = new ST.Type((String)null);
	        }else {
	        	//TODO: check is this right
	        	Object a = CG.genArray(new ST.Array(z, x_val, y_val - x_val + 1));

	        	x = new ST.Type(a);
	        	
	        }
	    }else if (SC.sym == SC.RECORD){
	        SC.getSym();
	        ST.openScope();
	        typedIds(ST.Var.class);
	        while (SC.sym == SC.SEMICOLON) {
	            SC.getSym();
	            typedIds(ST.Var.class);
	        }
	        if (SC.sym == SC.END)
	            SC.getSym();
	        else
	            SC.mark("'end' expected");
	        ArrayList<?> r = ST.topScope(); ST.closeScope();
	        Object a = CG.genRec(new ST.Record(r));
	        //TODO: do we need more cases
	        if (Helper.type(a).equals("Int"))
	        	x = new ST.Type(ST.Int.class);
	        else if (Helper.type(a).equals("Bool"))
	        	x = new ST.Type(ST.Bool.class);
	        else if (Helper.type(a).equals("Record"))
	        	x = new ST.Type(a);
	        else if (Helper.type(a).equals("Array"))
	        	x = new ST.Type(ST.Array.class);	//TODO: i think this wrong, try startsWith
	    }else {
			x = new ST.Type((String)null);
		}
			
		
		return x;
	}
	
	/*
	The function typeIds(kind) parses the production in the structure.

	typedIds ::= ident {"," ident} ":" type.

	It updates the top scope of symbol table; an error is reported if an identifier is already in the top scope.
	The parameter kind is assumed to be callable and applied to the type before an identifier and its type are entered in the symbol table.
	*/
	public static void typedIds(Class<?> kind) {

		ArrayList<Object> tid = new ArrayList<Object>();
		if (SC.sym == SC.IDENT) {
			tid.add(SC.val);
			SC.getSym();
		}else {
			SC.mark("identifier expected"); 
			tid.clear();
		}
		while (SC.sym == SC.COMMA) {
			SC.getSym();
			if (SC.sym == SC.IDENT) {
				tid.add(SC.val);
				SC.getSym();
			}else {
				SC.mark("identifier expected"); 
			}
		}
		String kindName = kind.getSimpleName();
		if (SC.sym == SC.COLON) {
			SC.getSym();
			Object tpObj = typ();
			String tp = (String) Helper.getProperty(tpObj, "getVal");

			if (tp != null) {
				for (Object i : tid) 
					ST.newDecl(i, kindName+"("+ tp+")");
			}
		}else {
			SC.mark("':' expected");
		}

	}

	/*
	The function declarations() parses the following production:

	declarations ::=
	    {"const" ident "=" expression ";"}
	    {"type" ident "=" type ";"}
	    {"var" typedIds ";"}
	    {"procedure" ident ["(" [["var"] typedIds {";" ["var"] typedIds}] ")"] ";"
	        declarations compoundStatement ";"}

	This function also updates the top scope of the symbol table;
	If a symbol/identifier already exists in the symbol table, then an appropriate an error is reported.
	An error is also returned if the parsed expression that is a declared as a constant declaration is not is fact
	a constant type. For each function, a new scope is opened, so that its formal parameters and local declarations,
	are added to the symbol table. COde is generated for the body of the declaration.
	The size of the variable declarations is returned, as determined by calling parameter function parameter allocVar.
	*/
	public static void declarations() {

		if (!FIRSTDECL.contains(SC.sym) && !FOLLOWDECL.contains(SC.sym) ){
			SC.mark("'begin' or declaration expected");
		    while (!FIRSTDECL.contains(SC.sym) && !FOLLOWDECL.contains(SC.sym) && !STRONGSYMS.contains(SC.sym)) {
		    	SC.getSym();
		    }
		}
		 
		while (SC.sym == SC.CONST) {
			SC.getSym();
			if (SC.sym == SC.IDENT) {
				String ident = SC.val; 
				SC.getSym();
		         
				if (SC.sym == SC.EQ)
					SC.getSym();
		        else
		        	SC.mark("= expected");
		         
				Object x = expression();
			    if (Helper.type(x).equals("Const"))
			    	ST.newDecl(ident, x.toString());
			    else
			    	SC.mark("expression not constant");
			} else {
				SC.mark("constant name expected");
			}
	        if (SC.sym == SC.SEMICOLON)
	        	SC.getSym();
	        else
	        	SC.mark("; expected");
		}
		 
		while (SC.sym == SC.TYPE) {
			SC.getSym();
			 
			if (SC.sym == SC.IDENT) {
				String ident = SC.val; SC.getSym();
				if (SC.sym == SC.EQ)
					SC.getSym();
		        else
		        	SC.mark("= expected");
	            
	            Object x = typ();

	            ST.newDecl(ident, x.toString());
	            
	            if (SC.sym == SC.SEMICOLON)
	            	SC.getSym();
	            else
	            	SC.mark("; expected");
			}else {
				 SC.mark("type name expected");
			}
		}
		start = ST.topScope().size();

		while (SC.sym == SC.VAR) {
			SC.getSym(); 
			typedIds(ST.Var.class);
			if (SC.sym == SC.SEMICOLON)
				SC.getSym();
			else
				SC.mark("; expected");
		}

		if (!CGwat.createdGlobalVars) {
			CG.genGlobalVars(ST.topScope(), start);
			CGwat.createdGlobalVars = false;
		}
		else 
			CG.genLocalVars(ST.topScope(), start);
		
		while (SC.sym == SC.PROCEDURE){
	        SC.getSym();
	        if (SC.sym == SC.IDENT)
	            SC.getSym();
	        else
	            SC.mark("procedure name expected");

	        String ident = SC.val;
	        ST.newDecl(ident, "Proc([])");
	        ArrayList <?> sc = ST.topScope();
	        ST.openScope();

	        ArrayList <?> fp;
	        if (SC.sym == SC.LPAREN){
	            SC.getSym();
	            if (SC.sym == SC.VAR || SC.sym == SC.IDENT) {
	                if (SC.sym == SC.VAR) {
	                    SC.getSym();
	                    typedIds(ST.Ref.class);
	                } else {
	                    typedIds(ST.Var.class);
	                }
	                while (SC.sym == SC.SEMICOLON) {
	                    SC.getSym();
	                    if (SC.sym == SC.VAR) {
	                        SC.getSym();
	                        typedIds(ST.Ref.class);
	                    } else {
	                        typedIds(ST.Var.class);
	                    }
	                }
	            }else {
	                SC.mark("formal parameters expected");
	            }
	            fp = ST.topScope();

	            Helper.setProperty(sc.get(sc.size()-1), "setPar", new ArrayList<>(fp));

	            if (SC.sym == SC.RPAREN)
	                SC.getSym();
	            else
	                SC.mark(") expected");
	        }else
	            fp = new ArrayList<Object>();


	        Object parsize = CG.genProcStart(ident, fp);
	        if (SC.sym == SC.SEMICOLON)
	            SC.getSym();
	        else
	            SC.mark("; expected");

//	       
	        declarations();
	        CG.genProcEntry(ident, parsize, null);
	        
	        Object x = compoundStatement(); CG.genProcExit(x, parsize, null);
	        ST.closeScope();
	        if (SC.sym == SC.SEMICOLON)
	            SC.getSym();
	        else
	            SC.mark("; expected");
	    }
		
		//return varsize;
	}

	/*
	The function below parses the production

	    program ::= "program" ident ";" declarations compoundStatement

	If there is no error, then the generated code is returned. The standard identifiers are entered initially in the symbol table.
	*/
	public static String program() {

		ST.newDecl("boolean", "Type(Bool)");
		ST.newDecl("integer", "Type(Int)");
		ST.newDecl("true", "Const(Bool, 1)");
		ST.newDecl("false", "Const(Bool, 0)");
		ST.newDecl("read", "StdProc([\"Ref(Int)\"])");	//TODO: this is null
		ST.newDecl("write", "StdProc([\"Var(Int)\"])");
		ST.newDecl("writeln", "StdProc([])");
	
		CG.genProgStart();

		if (SC.sym == SC.PROGRAM)
			SC.getSym();
		else
			SC.mark("'program' expected");

		String ident = SC.val;

		if (SC.sym == SC.IDENT)
			SC.getSym();
		else
			SC.mark("program name expected");

		if (SC.sym == SC.SEMICOLON)
			SC.getSym();
		else
			SC.mark("; expected");
		ST.printSymTab();
		declarations();
		CG.genProgEntry(ident);
		Object x = compoundStatement();
		return CG.genProgExit(x);
	}
	
	/*
	This functions compiles the source program that is given bu the variable str.
	*/
	public static void compileString(String src, File dstfn, String target) {
		SC.init(src);
		ST.init();
		String p = program();
		if (p != null && !SC.error) {
			System.out.println(p);
		}else {
			System.out.println("Cant print due to error or p is null...printing");
//			System.out.println(p);
		}
		
	}
}

