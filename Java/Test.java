import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Test {

	/*
	 */
	public static void main(String[] args) {

		long startTime = System.nanoTime();
		for (int i=0; i<100; i++) {
			String src = " program p;\r\n" + 
					"        var x, y, z, q: integer;\r\n" + 
					"        procedure function(x, y: integer);\r\n" + 
					"          begin q := 0;\r\n" + 
					"            while q <= 100 do\r\n" + 
					"              begin q := q + 1;\r\n" + 
					"                z := 3;\r\n" + 
					"                z := x + y * 75 - 12;\r\n" + 
					"                if z > 0\r\n" + 
					"                then x := 100\r\n" + 
					"                else x := 50;\r\n" + 
					"                if z < 100\r\n" + 
					"                then z := 46\r\n" + 
					"                else z := 13\r\n" + 
					"              end;\r\n" + 
					"            write(q); writeln\r\n" + 
					"          end;\r\n" + 
					"        begin\r\n" + 
					"            if x>0 then write(7) else write(9);\r\n" + 
					"            if z>100 then write(45) else write(0);\r\n" + 
					"          read(x); read(y);\r\n" + 
					"          function(x, y)\r\n" + 
					"        end\n";
				
			P0.compileString(src, null, "wat");
		}
		long endTime = System.nanoTime();
		long durationInNano = (endTime - startTime);  //Total execution time in nano seconds
		long durationInMillis = TimeUnit.NANOSECONDS.toMillis(durationInNano);  //Total execution time in nano seconds
	    System.out.println("The execution time is: " + durationInMillis + "ms");
	}
	
	

	public static String scan_src(String src) {
		String str ="[";
	    SC.init(src);
	    str += "(" + SC.sym + ", " + SC.val + "),";
	    while (SC.sym != SC.EOF) {
	        SC.getSym();
	        str += "(" + SC.sym + ", " + SC.val + "),";
		    
	    }
	    str+="]";
	    return str;
	}
	public static void test_scanner(String src) {
	    String result = scan_src(src);
	    System.out.println(src +": " + result);

	}
}
