library DartParser;

import 'package:DartParser/SC.dart';
import 'package:DartParser/P0.dart';

void main() {
    Stopwatch stopwatch = new Stopwatch()..start();

    for (int i=0; i<100;i++){
        compileString("""
        program p;
        var x, y, z, q: integer;
        procedure function(x, y: integer);
          begin q := 0;
            while q <= 100 do
              begin q := q + 1;
                z := 3;
                z := x + y * 75 - 12;
                if z > 0
                then x := 100
                else x := 50;
                if z < 100
                then z := 46
                else z := 13
              end;
            write(q); writeln
          end;
        begin
            if x>0 then write(7) else write(9);
            if z>100 then write(45) else write(0);
          read(x); read(y);
          function(x, y)
        end
            """);
    }
    print('The execution time is: ${stopwatch.elapsedMilliseconds}ms' );

}



String scan_src(String src) {
    String str ="[";
    init(src);
    str += "(${sym}, ${val}),";
    while (sym != EOF) {
        getSym();
        str += "(${sym}, ${val}),";
    }
    str+="]";

    return str;
}
void test_scanner(String src) {
    String result = scan_src(src);
    print(src +": " + result);

}
