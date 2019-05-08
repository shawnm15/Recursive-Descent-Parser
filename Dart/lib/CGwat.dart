/// Support for doing something awesome.
///
/// More dartdocs go here.
library CGwat;

import 'package:DartParser/SC.dart';
import 'package:DartParser/ST.dart';
import 'package:DartParser/P0.dart';

int curlev, memsize;
var asm;

/*
Function that initializes global vars. Creates the list that will store the generated code.
*/
void genProgStart() {

    curlev = memsize = 0;

    asm = ["(module",
            "(import \"P0lib\" \"write\" (func \$write (param i32)))",
            "(import \"P0lib\" \"writeln\" (func \$writeln))",
            "(import \"P0lib\" \"read\" (func \$read (result i32)))"];
}

/*
Following procedures "generate code" for all P0 types by determining the size of objects and store in the size field.

Integers and booleans occupy 4 bytes
The size of a record is the sum of the sizes of its field; the offset of a field is the sum of the size of the preceding fields
The size of an array is its length times the size of the base type.
*/
dynamic genBool(dynamic b){
    b.size = 1;
    return b;
}

dynamic genInt(dynamic i){
    i.size = 4;
    return i;
}

dynamic genRec(dynamic r){
    var s = 0;
    var temp = 0;
    for (dynamic f in r.fields) {
        try{
            f.offset = s;
            if (f.tp != null){
                temp= 1;
            }
            s = s + temp;
        }on NoSuchMethodError{
            //do nothing
        }
    }
    try{
        r.size = s;
    }on NoSuchMethodError{
        //do nothing
    }
    return r;
}

dynamic genArray(dynamic a){

    try{
        a.size = a.length * a.base.size;
    }on NoSuchMethodError{
        a.size = a.length * 1;
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
void genGlobalVars(dynamic sc, int start){
    for (int i=start; i<sc.length; i++) {
        if (type(sc[i]) == Var) {
            if (sc[i].tp == Int || sc[i].tp == Bool) {
                asm.add("(global \$${sc[i].name} (mut i32) i32.const 0)");
            }else if (sc[i].tp is Array || sc[i].tp is  Record) {
                sc[i].lev = -2;
                sc[i].adr = memsize;
                memsize = sc[i].tp.size;
            }else {
                mark("WASM: type?");
            }
        }
    }
}

dynamic genLocalVars(dynamic sc, int start){
    for (int i=start; i<sc.length; i++) {
        if (type(sc[i]) == Var) {
            if (sc[i].tp == Int || sc[i].tp == Bool) {
                asm.add("(local \$${sc[i].name} i32)");
            }else if (sc[i].tp is Array || sc[i].tp is  Record) {
                mark('WASM: no local arrays, records');
            }else {
                mark("WASM: type?");
            }
        }
    }
    return null;
}

/*
Procedure loadItem(x) generates code for loading x on the expression stack, assuming x is global Var,
local Var, stack Var, memory Var, local Ref, stack Ref, Const.
*/
void loadItem(dynamic x) {
    if (type(x) == Var) {
        if (x.lev == 0) {
            asm.add("global.get \$${x.name}");
        }else if (x.lev == curlev) {
            asm.add("local.get \$${x.name}");
        }else if (x.lev == -2) {
            asm.add("i32.const ${x.adr}");
            asm.add("i32.load");
        }else if (x.lev != -1)
            mark("WASM: var level!");
    }else if (type(x) == Ref) {
        if (x.lev == -1) {
            asm.add("i32.load");
        }else if (x.lev == curlev) {
            asm.add("local.get \$${x.name}");
            asm.add("i32.load");
        }else {
            mark("WASM: ref level!");
        }
    }else if (type(x) == Const) {
        asm.add("i32.const ${x.val}");
    }
}


dynamic genVar(dynamic x){
    dynamic y;
    if (0 < x.lev && x.lev < curlev) {
        mark('WASM: level!');
    }
    if (type(x) == Ref) {
        y = Ref(x.tp);
        y.lev = x.lev;
        y.name = x.name;
    }else if (type(x) == Var) {
        y = Var(x.tp);
        y.lev = x.lev;
        y.name = x.name;
    }
    if (x.lev == -2) {
        y.adr = x.adr;
    }
    return y;
}
/*
Procedure genConst(x) does not need to generate any code.
*/
dynamic genConst(dynamic x){
    return x;
}

/*
Procedure genUnaryOp(op, x) generates code for op x if op is MINUS, NOT and x is Int, Bool, respectively.
If op is AND, OR, item x is the first operand and an if instruction is generated.
*/
dynamic genUnaryOp(int op, dynamic x){
    loadItem(x);
    if (op == MINUS) {
        asm.add('i32.const -1');
        asm.add('i32.mul');
        x = Var(Int);
        x.lev = -1;
    }else if (op == NOT){
        asm.add('i32.eqz');
        x = Var(Bool); x.lev = -1;
    }else if (op == AND) {
        asm.add('if (result i32)');
        x = Var(Bool);
        x.lev = -1;
    }else if (op == OR){
        asm.add('if (result i32)');
        asm.add('i32.const 1');
        asm.add('else');
        x = Var(Bool); x.lev = -1;
    }else {
        mark('WASM: unary operator?');
    }
    return x;
}
/*
Procedure genBinaryOp(op, x, y) generates code for x op y if op is PLUS, MINUS, TIMES, DIV, MOD. If op is AND, OR, code for x and the start of an if
instruction has already been generated; code for y and the remainder of the if instruction is generated.
*/
dynamic genBinaryOp(op, dynamic x, dynamic y){
    if ({PLUS, MINUS, TIMES, DIV, MOD}.contains(op)){
        loadItem(x); loadItem(y);
        if (op == PLUS){
            asm.add('i32.add');
        }else if (op == MINUS){
            asm.add('i32.sub');
        }else if (op == TIMES){
            asm.add('i32.mul');
        }else if (op == DIV){
            asm.add('i32.div_s');
        }else if (op == MOD){
            asm.add('i32.rem_s');
        }else{
            asm.add('?');
        }

        x = Var(Int); x.lev = -1;
    }else if (op == AND){
        loadItem(y);
        asm.add('else');
        asm.add('i32.const 0');
        asm.add('end');
        x = Var(Bool); x.lev = -1;
    }else if (op == OR){
        loadItem(y);
        asm.add('end');
        x = Var(Bool); x.lev = -1;
    }else {
        assert(false);
    }
    return x;
}
/*
Procedure `genRelation(op, x, y)` generates code for `x op y` if `op` is `EQ`, `NE`, `LT`, `LE`, `GT`, `GE`.
*/
dynamic genRelation(op, dynamic x, dynamic y){

    loadItem(x); loadItem(y);
    if (op == EQ){
        asm.add('i32.eq');
    }else if (op == NE){
        asm.add('i32.ne');
    }else if (op == LT){
        asm.add('i32.lt_s');
    }else if (op == GT){
        asm.add('i32.gt_s');
    }else if (op == LE){
        asm.add('i32.le_s');
    }else if (op == GE){
        asm.add('i32.ge_s');
    }else{
        asm.add('?');
    }

    x = Var(Bool); x.lev = -1;

    return x;
}

/*
Procedure genSelect(x, f) generates code for x.f, provided f is in x.fields. If x is Var, i.e. allocated in memory, only x.adr is updated and no code is generated.
If x is Ref, i.e. a reference to memory, code for adding the offset of f is generated. An updated item is returned.
*/
dynamic genSelect(dynamic x, dynamic f){
    if (type(x) == Var) {
        x.adr += f.offset;
    }else if (type(x) == Ref){
        if (x.lev > 0) {
            asm.add('local.get \$${x.name}');
        }
        asm.add('i32.const ${f.offset}');
        asm.add('i32.add');
        x.lev = -1;
    }

    x.tp = f.tp;
    return x;

}
/*
Procedure genIndex(x, y) generates code for x[y], assuming x is Var or Ref, x.tp is Array, and y.tp is Int.
If y is Const, only x.adr is updated and no code is generated, otherwise code for array index calculation is generated.
*/
dynamic genIndex(dynamic x, dynamic y){
    if (type(x) == Var){
        if (type(y) == Const) {
            if (x.tp.base == Int || x.tp.base == Bool)
                x.adr += (y.val - x.tp.lower) * 1;
            else
                x.adr += (y.val - x.tp.lower) * x.tp.base.size;
            x.tp = x.tp.base;
        }else{
            loadItem(y);
            if (x.tp.lower != 0){
                asm.add('i32.const ${x.tp.lower}');
                asm.add('i32.sub');
            }
            try{
                asm.add('i32.const ${x.tp.base.size}');
            }on NoSuchMethodError{
                asm.add('i32.const ${1}');
            }
            asm.add('i32.mul');
            asm.add('i32.const ${x.adr}');
            asm.add('i32.add');
            x = Ref(x.tp.base); x.lev = -1;
        }
    }else{
        if (x.lev == curlev) {
            loadItem(x);
            x.lev = -1;
        }
        if (type(y) == Const) {
            if (x.tp.base == Int || x.tp.base == Bool)
                asm.add('i32.const ${(y.val - x.tp.lower) * 1}');
            else
                asm.add('i32.const ${(y.val - x.tp.lower) * x.tp.base.size}');
            asm.add('i32.add');
        }else {
            loadItem(y);
            asm.add('i32.const ${x.tp.lower}');
            asm.add('i32.sub');
            if (x.tp.base == Int || x.tp.base == Bool)
                asm.add('i32.const 1');
            else
                asm.add('i32.const ${x.tp.base.size}');
            asm.add('i32.mul');
            asm.add('i32.add');
        }
        x.tp = x.tp.base;
    }

    return x;
}

/*
Procedure `genAssign(x, y)` generates code for `x := y`, provided `x` is `Var`, `Ref` and `y` is `Var`, `Ref`.
*/
dynamic genAssign(dynamic x, dynamic y) {
    if (type(x) == Var) {
        if (x.lev == -2)
            asm.add("i32.const ");

        loadItem(y);
        if (x.lev == 0)
            asm.add("global.set \$${x.name}" );
        else if (x.lev == curlev) {
            asm.add("local.set \$${x.name}");
        }else if (x.lev == -2) {
            asm.add("i32.store");
        }else if (x.lev != -1)
            mark("WASM: level!");
    }else if (type(x) == Ref) {
        if (x.lev == curlev)
            asm.add("local.get \$${x.name}");
        loadItem(y);
        asm.add("i32.store");
    }

    return null;
}

void genProgEntry(String ident) {
    asm.add("(func \$program");
}


String genProgExit(Object x) {
    asm.add(")\n(memory ${memsize ~/ expo(2,16) + 1})\n(start \$program)\n)");

    String result = "";
    for (String string in asm) {
        result += string + "\n";
    }
    return result;
}

dynamic genProcStart(String ident, dynamic fp){
    if (curlev > 0)
        mark('WASM: no nested procedures');
    curlev = curlev + 1;

    var str = "";

    for (dynamic e in fp){
        str += '(param \$${e.name} i32) ';
    }
    asm.add('(func \$$ident ${str}');

    for (dynamic p in fp) {
        if ((p.tp == Int || p.tp == Bool) && type(p) == Ref)
            mark('WASM: only array and record reference parameters');
        else if ((type(p.tp) == Array || type(p.tp)== Record) && type(p) == Var)
            mark('WASM: no structured value parameters');
    }


}


void genProcEntry(dynamic ident, dynamic parsize, dynamic localsize){
    //pass
}

void genProcExit(dynamic ident, dynamic parsize, dynamic localsize){
    curlev = curlev - 1;
    asm.add(')');
}


dynamic genActualPara(dynamic ap, dynamic fp, int n) {
    if (type(fp) == Ref) {
        if (ap.lev == -2)
            asm.add("i32.const ${ap.adr}");
    }else if ({Var, Ref, Const}.contains(type(ap))){
        loadItem(ap);
    }else
        mark("unsupported parameter type");
    return null;
}

/*

*/
dynamic genCall(dynamic pr, dynamic ap) {
    asm.add("call \$${pr.name}");
    return null;
}

/*

*/
dynamic genRead(dynamic x) {
    asm.add('call \$read');
    dynamic y = Var(Int); y.lev = -1;
}

/*

*/
dynamic genWrite(dynamic x) {
    loadItem(x);
    asm.add("call \$write");
    return null;
}

/*

*/
dynamic genWriteln() {
    asm.add("call \$writeln");
    return null;
}

dynamic genSeq(dynamic x, dynamic y) {
    return null;
}

dynamic genThen(dynamic x) {
    loadItem(x);
    asm.add("if");
    return x;
}

dynamic genIfThen(dynamic x, dynamic y) {
    asm.add("end");
    return null;
}

dynamic genElse(dynamic x, dynamic y) {
    asm.add("else");
    return null;
}

dynamic genIfElse(dynamic x, dynamic y, dynamic z) {
    asm.add("end");
    return null;
}

dynamic genWhile() {
    asm.add("loop");
    return null;
}


dynamic genDo(dynamic x) {
    loadItem(x);
    asm.add("if");
    return x;
}

dynamic genWhileDo(dynamic t, dynamic x, dynamic y) {
    asm.add("br 1");
    asm.add("end");
    asm.add("end");
    return null;
}

