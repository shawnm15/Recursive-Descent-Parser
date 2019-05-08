import java.util.HashMap;
import java.util.Map;
import static java.util.Map.entry;


public class SC {
	
	public static int TIMES = 1;public static int DIV = 2;public static  int MOD = 3;
	public static int AND = 4;public static  int PLUS = 5;public static int  MINUS = 6;
	public static int OR = 7;public static int EQ = 8;public static int NE = 9;public static int LT = 10;
	public static int GT = 11;public static int LE = 12;public static int GE = 13; public static int PERIOD = 14;
	public static int COMMA = 15;public static int COLON = 16;public static int RPAREN = 17;
	public static int RBRAK = 18;public static int OF = 19;public static int THEN = 20;public static int DO = 21;
	public static int LPAREN = 22;public static int LBRAK = 23;public static int NOT = 24;
	public static int BECOMES = 25;public static int NUMBER = 26;public static int IDENT = 27;
	public static int SEMICOLON = 28;public static int END = 29;public static int ELSE = 30;
	public static int IF = 31;public static int WHILE = 32;public static int ARRAY = 33;
	public static int RECORD = 34;public static int CONST = 35;public static int TYPE = 36;
	public static int VAR = 37;public static int PROCEDURE = 38;public static int BEGIN = 39;
	public static int PROGRAM = 40;public static int EOF = 41;
	
	public static int line, lastline, errline, pos, lastpos, errpos, sym, index;
	public static boolean error;
	public static String source,val;
	public static char ch;
	
	public static void init(String src) {
		
		line = lastline = errline = 1;
		pos = lastpos = errpos = 0;
		sym = 0;
		val = null;
		error = false;
		source = src;
		index = 0;
		getChar(); getSym();
	}
	
	

	public static void getChar() {
	    
	    if (index == source.length()){
	    	ch = '\0';
	    }
	    else {
	        ch = source.charAt(index);
	        index++;
	        lastpos = pos;
	        if (ch == '\n') {
	            pos = 0;
	        	line++;
	        }else {
	            lastline = line;
	        	pos++;
	        }
	    }
	}
	
	public static void mark(String msg) {
		if (lastline > errline || lastpos > errpos) 
			System.out.println("error: line " + lastline + " pos " + lastpos + " " + msg);
		
		errline = lastline;
		errpos = lastpos; 
		error = true;
	}

	public static void number() {
		sym = NUMBER;
		val = "0";
		
		while ('0' <= ch && ch <='9') {
			
			val = Integer.toString(10 * Integer.parseInt(val) + Character.getNumericValue(ch));
			getChar();
		}
		if (Integer.parseInt(val) >= Math.pow(2, 31)) {
			mark("number too large"); 
			val = "0";
		}
	}
	

	public static Map<String, Integer> KEYWORDS = Map.ofEntries(
	    entry("div", DIV),
	    entry("mod", MOD),
	    entry("and", AND),
	    entry("or", OR),
	    entry("of", OF),
	    entry("then", THEN),
	    entry("do", DO),
	    entry("not", NOT),
	    entry("end", END),
	    entry("else", ELSE),
	    entry("if", IF),
	    entry("while", WHILE),
	    entry("array", ARRAY),
	    entry("record", RECORD),
	    entry("const", CONST),
	    entry("type", TYPE),
	    entry("var", VAR),
	    entry("procedure", PROCEDURE),
	    entry("begin", BEGIN),
	    entry("program", PROGRAM)
	);
	

	public static void identKW() {
		int start = index - 1;
		
		while ('A' <= ch && ch <= 'Z' || 'a' <= ch && ch <= 'z' || '0' <= ch && ch <= '9' )
			getChar();
		
		val = source.substring(start, index-1);
		if (KEYWORDS.containsKey(val)) 
			sym = KEYWORDS.get(val);
		else
			sym = IDENT;
	}
	
	public static void comment() {
		while ('\0' != ch && ch != '}')
			getChar();
		
		if (ch == '\0')
			mark("comment not terminated");
		else
			getChar();
	}
	
	public static void getSym() {
		while ('\0' < ch && ch <= ' ')
			getChar();
		
		if (('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z')) {
			identKW();
		}else if ('0' <= ch && ch <= '9') {
	    	number();
	    }else if (ch == '{') {
	    	comment(); getSym();
	    }else if (ch == '*') {
	    	getChar(); sym = TIMES;
	    }else if (ch == '+') {
	    	getChar(); sym = PLUS;
		}else if (ch == '-') {
	    	getChar(); sym = MINUS;
		}else if (ch == '=') { 
	    	getChar(); sym = EQ;
		}else if (ch == '<') {
	        getChar();
	        if (ch == '=') {
	        	getChar(); sym = LE;
	        }else if (ch == '>') { 
	        	getChar(); sym = NE;
	        }else
	        	sym = LT;
		}else if (ch == '>') {
	        getChar();
	        if (ch == '=') {
	        	getChar(); sym = GE;
	        }else
	        	sym = GT;
		}else if (ch == ';') {
	    	getChar(); sym = SEMICOLON;
	    }else if (ch == ',') { 
	    	getChar(); sym = COMMA;
	    }else if (ch == ':') {
	        getChar();
	        if (ch == '=') {
	        	getChar(); sym = BECOMES;
	        }else
	        	sym = COLON;
	    }else if (ch == '.') {
	    	getChar(); sym = PERIOD;
	    }else if (ch == '(') {
	    	getChar(); sym = LPAREN;
	    }else if (ch == ')') {
	    	getChar(); sym = RPAREN;
	    }else if (ch == '[') {
	    	getChar(); sym = LBRAK;
	    }else if (ch == ']') {
	    	getChar(); sym = RBRAK;
	    }else if (ch == '\0') {
	    	sym = EOF;
	    }else {
	    	mark("illegal character"); getChar(); sym = 0;
	    }
		
	}

}
