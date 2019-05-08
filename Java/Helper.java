import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class Helper {

	
	public static Object getProperty(Object x, String property) {
		
		Object result = null;
		
		try {
			Method getMethod = x.getClass().getMethod(property);
			result = getMethod.invoke(x);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		
		return result;
	}
	
	
	public static void setProperty(Object x, String property, Object arg) {
		
		Method setMethod = null;
		Object result = null;
		
		try {
			if (arg instanceof Integer) 
				setMethod = x.getClass().getMethod(property, int.class);
			else if (arg instanceof Boolean)
				setMethod = x.getClass().getMethod(property, boolean.class);
			else if (arg instanceof String)
				setMethod = x.getClass().getMethod(property, String.class);
			else
				setMethod = x.getClass().getMethod(property, arg.getClass());

			setMethod.invoke(x, arg);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		
	}

	public static Integer toInt(String arg) {
		return Integer.parseInt(arg);
	}
	
	public static Boolean toBool(String arg) {
		return Boolean.parseBoolean(arg);
	}
	
	public static String toStr(boolean arg) {
		return String.valueOf(arg);
	}

	public static String type(Object x) {
		return x.getClass().getSimpleName();
	}
	
	public static ArrayList<?> toList(String value) {
		
		String debracketed = value.replace("[", "").replace("]", ""); // now be "foo, bar, baz"
		String trimmed = debracketed.replaceAll("\\s+", ""); // now is "foo,bar,baz"
		ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(trimmed.split(","))); // now have an ArrayList containing "foo", "bar" and "baz"
		return list;
	}
	
	public static String [] getFieldProp(String s) {
		String line = s.substring(s.indexOf("["), s.indexOf("]")+1);
		
        String notBracket = " [^\\(\\)] ";
        String bracketString = String.format(" \\( %s* \\) ", notBracket);
        String regex = String.format("(?x) "+ 
                ",                         "+ 
                "(?=                       "+ 
                "  (?:                     "+ 
                "    %s*                   "+ 
                "    %s                    "+ 
                "  )*                      "+ 
                "  %s*                     "+ 
                "  $                       "+ 
                ")                         ", 
                notBracket, bracketString, notBracket);

        return line.split(regex, -1);
	}
	public static String getVal(String searchString, String field) {
		return searchString.substring(searchString.indexOf(field)+field.length()+3, searchString.indexOf(","));
	}
}
