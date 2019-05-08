library P0;

import 'package:DartParser/SC.dart' as SC show init, sym, val, error,
TIMES, DIV, MOD, AND, PLUS, MINUS, OR, EQ, NE, LT, GT,
LE, GE, PERIOD, COMMA, COLON, RPAREN, RBRAK, OF, THEN, DO, LPAREN,
LBRAK, NOT, BECOMES, NUMBER, IDENT, SEMICOLON, END, ELSE, IF, WHILE,
ARRAY, RECORD, CONST, TYPE, VAR, PROCEDURE, BEGIN, PROGRAM, EOF, getSym, mark;
import 'package:DartParser/ST.dart';
import 'package:DartParser/CGwat.dart' as CG;

var start;

/*
The below declarations are the first and follows sets for resucrive descent parsing that will used throughout
many of the functions below.
*/
var FIRSTFACTOR = {SC.IDENT, SC.NUMBER, SC.LPAREN, SC.NOT};
var FOLLOWFACTOR = {SC.TIMES, SC.DIV, SC.MOD, SC.AND, SC.OR, SC.PLUS, SC.MINUS, SC.EQ, SC.NE, SC.LT, SC.LE, SC.GT, SC.GE,
SC.COMMA, SC.SEMICOLON, SC.THEN, SC.ELSE, SC.RPAREN, SC.RBRAK, SC.DO, SC.PERIOD, SC.END};
var FIRSTEXPRESSION = {SC.PLUS, SC.MINUS, SC.IDENT, SC.NUMBER, SC.LPAREN, SC.NOT};
var FIRSTSTATEMENT = {SC.IDENT, SC.IF, SC.WHILE, SC.BEGIN};
var FOLLOWSTATEMENT = {SC.SEMICOLON, SC.END, SC.ELSE};
var FIRSTTYPE = {SC.IDENT, SC.RECORD, SC.ARRAY, SC.LPAREN};
var FOLLOWTYPE = {SC.SEMICOLON};
var FIRSTDECL = {SC.CONST, SC.TYPE, SC.VAR, SC.PROCEDURE};
var FOLLOWDECL = {SC.BEGIN};
var FOLLOWPROCCALL = {SC.SEMICOLON, SC.END, SC.ELSE};
var STRONGSYMS = {SC.CONST, SC.TYPE, SC.VAR, SC.PROCEDURE, SC.WHILE, SC.IF, SC.BEGIN, SC.EOF};

/*
This functions helps understand and generate code for selector expressions. A selector looks like
x[i] or x.f. The passed parameter of x is the identifier in front of the selector. Depending on the type of
the selector, the appropriate code is generated in the CGWat.
 */
dynamic selector(dynamic x){
    while (SC.sym == SC.PERIOD || SC.sym == SC.LBRAK ) {
        if (SC.sym == SC.PERIOD) {
            SC.getSym();
            if (SC.sym == SC.IDENT) {
                if (type(x.tp) == Record) {
                    var found = false;
                    for (dynamic f in x.tp.fields) {
                        if (f.name == SC.val) {
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
            SC.getSym(); dynamic y = expression();
            if (type(x.tp) == Array)
                if (y.tp == Int)
                    if (type(y) == Const && (y.val < x.tp.lower || y.val >= x.tp.lower + x.tp.length))
                        SC.mark('index out of bounds');
                    else x = CG.genIndex(x, y);
                else SC.mark('index not integer');
            else SC.mark('not an array');

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
dynamic factor(){
    dynamic x;
    if (!FIRSTFACTOR.contains(SC.sym)) {
        SC.mark("expression expected");
        while (!FIRSTFACTOR.contains(SC.sym) && !FOLLOWFACTOR.contains(SC.sym) && !STRONGSYMS.contains(SC.sym)) {
            SC.getSym();
        }
    }
    if (SC.sym == SC.IDENT) {
        x = find(SC.val);
        if (type(x) == Var || type(x) == Ref) {
            x = CG.genVar(x); SC.getSym();
        }else if (type(x) == Const) {
            x = Const(x.tp, x.val); x = CG.genConst(x); SC.getSym();
        }else {
            SC.mark("expression expected");
        }
        x = selector(x);
    }else if (SC.sym == SC.NUMBER) {
        x = Const(Int, SC.val); x = CG.genConst(x); SC.getSym();
    }else if (SC.sym == SC.LPAREN) {
        SC.getSym(); x = expression();
        if (SC.sym == SC.RPAREN)
            SC.getSym();
        else
            SC.mark(") expected");
    }else if (SC.sym == SC.NOT){
        SC.getSym(); x = factor();
        if (x.tp != Bool)
            SC.mark("not boolean");
        else if (type(x) == Const) {
            x.val = 1 - x.val;
        }else
            x = CG.genUnaryOp(SC.NOT, x);
    }else {
        x = Const(null, 0);
    }

    return x;
}

/*
The term function parses grammar in the form 'term ::= factor {("*" | "div" | "mod" | "and") factor}'
It will generate code for the term. If the term is a constant, then we call a Const item is returned
(and code may not need to be generated).
If the term is not a constant, the location of the result is returned as determined by the code generator.'
 */
dynamic term(){
    dynamic x = factor();
    dynamic y;
    while (SC.sym == SC.TIMES || SC.sym == SC.DIV || SC.sym == SC.MOD || SC.sym == SC.AND) {
        int op = SC.sym; SC.getSym();
        if (op == SC.AND && type(x) != Const)
            x = CG.genUnaryOp(SC.AND, x);
        y = factor();

        if ((x.tp == Int && Int == y.tp) && (op == SC.TIMES || op == SC.DIV || op == SC.MOD)) {
            if (type(x) == Const && Const == type(y)) {
                if (op == SC.TIMES)
                    x.val = x.val * y.val;
                else if (op == SC.DIV) {
                    x.val = x.val ~/ y.val;
                }else if (op == SC.MOD)
                    x.val = x.val % y.val;
            }else {
                x = CG.genBinaryOp(op, x, y);
            }
        }else if (x.tp == Bool && Bool == y.tp && op == SC.AND) {
            if (type(x) == Const){
                if (type(x.val)==int) {
                    if (x.val == 1)
                        x = y;
                }else if (type(x.val)==bool){
                    if (x.val)
                        x = y;
                }
            }else
                x = CG.genBinaryOp(SC.AND, x, y);
        }else
            SC.mark("bad type");
    }
    return x;
}

/*
This functions understands productions in the form of 'simpleExpression ::= ["+" | "-"] term {("+" | "-" | "or") term}'.
Code is generated in for the simple expression if no error is reported. If the simple expression is a constant,
a Const item is returned (and code may not need to be generated). Ihe the simple expression being parsed is not constant,
then the location of the result is returned as determined by the code generator module.
*/
dynamic simpleExpression(){
    dynamic x;
    dynamic y;
    if (SC.sym == SC.PLUS) {
        SC.getSym();
        x = term();
    }else if (SC.sym == SC.MINUS) {
        SC.getSym();
        x = term();

        if (x.tp != Int) {
            SC.mark("bad type");
        }else if (type(x) == Const) {
            x.val = - x.val;
        }else {
            x = CG.genUnaryOp(SC.MINUS, x);
        }
    }else {
        x = term();
    }

    while (SC.sym == SC.PLUS || SC.sym == SC.MINUS || SC.sym == SC.OR ) {
        int op = SC.sym; SC.getSym();
        if (op == SC.OR && type(x) != Const)
            x = CG.genUnaryOp(SC.OR, x);
        y = term();

        if (x.tp == Int && Int == y.tp && (op == SC.PLUS || op == SC.MINUS)) {
            if (type(x) == Const && Const == type(y)) {
                if (op == SC.PLUS) {
                    x.val = x.val + y.val;
                }else if (op == SC.MINUS) {
                    x.val = x.val - y.val;
                }
            }else
                x = CG.genBinaryOp(op, x, y);
        }else if (x.tp == Bool && Bool == y.tp && op == SC.OR) {
            if (type(x) == Const) {
                if (type(x.val)==int) {
                    if (x.val != 1)
                        x = y;
                }else if (type(x.val)==bool){
                    if (!x.val)
                        x = y;
                }
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

dynamic expression(){
    dynamic x = simpleExpression();

    while ({SC.EQ, SC.NE, SC.LT, SC.LE, SC.GT, SC.GE}.contains(SC.sym)) {
        int op = SC.sym;
        SC.getSym();
        dynamic y = simpleExpression();

        if (x.tp == y.tp && (y.tp == Int || y.tp == Bool)) {
            if (type(x) == Const && Const == type(y)) {
                if (op == SC.EQ) {
                    x.val = x.val == y.val;
                }else if (op == SC.NE) {
                    x.val = x.val != y.val;
                }else if (op == SC.LT) {
                    x.val = x.val < y.val;
                }else if (op == SC.LE) {
                    x.val = x.val <= y.val;
                }else if (op == SC.GT) {
                    x.val = x.val > y.val;
                }else if (op == SC.GE) {
                    x.val = x.val >= y.val;
                }
                x.tp = Bool;
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

dynamic compoundStatement(){
    if (SC.sym == SC.BEGIN) {
        SC.getSym();
    }else {
        SC.mark("'begin' expected");
    }
    dynamic x = statement();
    while (SC.sym == SC.SEMICOLON || FIRSTSTATEMENT.contains(SC.sym)) {
        if (SC.sym == SC.SEMICOLON)
            SC.getSym();
        else
            SC.mark("; missing");
        dynamic y = statement();
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
dynamic statement(){
    if (!FIRSTSTATEMENT.contains(SC.sym)) {
        SC.mark("statement expected"); SC.getSym();
        while (!FIRSTSTATEMENT.contains(SC.sym) && !FOLLOWSTATEMENT.contains(SC.sym) && !STRONGSYMS.contains(SC.sym)) {
            SC.getSym();
        }
    }
    dynamic x;
    dynamic y;
    if (SC.sym == SC.IDENT) {
        x = find(SC.val); SC.getSym();
        //TODO: change this to type(x) is Var || type(x) is Ref check and see if it works
        if ({Var, Ref}.contains(type(x))) {
            x = CG.genVar(x);
            x = selector(x);
            if (SC.sym == SC.BECOMES) {
                SC.getSym();
                y = expression();

                if (x.tp == y.tp && (y.tp == Int || y.tp == Bool))
                    x = CG.genAssign(x, y);
                else
                    SC.mark("incompatible assignment");


            }else if (SC.sym == SC.EQ) {
                SC.mark(":= expected");
                SC.getSym();
                y = expression();
            }else
                SC.mark(":= expected");
        }else if ({Proc, StdProc}.contains(type(x))) { //TODO: change this to type(x) is Var || type(x) is Ref check and see if it works

            dynamic fp = x.par; dynamic ap = []; int i=0;

            if (SC.sym == SC.LPAREN) {
                SC.getSym();
                if (FIRSTEXPRESSION.contains(SC.sym)) {
                    y = expression();

                    if (i < fp.length) {
                        if ((type(fp[i]) == Var || type(y) == Var) && fp[i].tp == y.tp) {
                            if (type(x) == Proc) {
                                ap.add(CG.genActualPara(y, fp[i], i));
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
                        if (i < fp.length) {
                            if ((type(fp[i]) == Var || type(y) == Var) && fp[i].tp == y.tp) {
                                if (type(x) == Proc) {
                                    ap.add(CG.genActualPara(y, fp[i], i));
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
            if (i < fp.length)
                SC.mark("too few parameters");
            else if (type(x) == StdProc) {
                if (x.name == 'read')
                    x = CG.genRead(y);
                else if  (x.name == 'write')
                    x = CG.genWrite(y);
                else if  (x.name == 'writeln')
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

        if (x.tp == Bool)
            x = CG.genThen(x);
        else
            SC.mark("boolean expected");

        if (SC.sym == SC.THEN)
            SC.getSym();
        else
            SC.mark("'then' expected");

        y = statement();

        if (SC.sym == SC.ELSE) {
            if (x.tp == Bool)
                y = CG.genElse(x, y);
            SC.getSym();
            dynamic z = statement();
            if (x.tp == Bool)
                x = CG.genIfElse(x, y, z);
        } else{
            if (x.tp == Bool)
                x = CG.genIfThen(x, y);
        }
    }else if (SC.sym == SC.WHILE) {
        SC.getSym();
        dynamic t = CG.genWhile();
        x = expression();

        if (x.tp == Bool)
            x = CG.genDo(x);
        else
            SC.mark("boolean expected");

        if (SC.sym == SC.DO)
            SC.getSym();
        else
            SC.mark("'do' expected");
        y = statement();
        if (x.tp == Bool)
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
dynamic typ(){
    if (!FIRSTTYPE.contains(SC.sym)) {
        SC.mark("type expected");
        while (!FIRSTTYPE.contains(SC.sym) && !FOLLOWTYPE.contains(SC.sym) && !STRONGSYMS.contains(SC.sym)) {
            SC.getSym();
        }
    }
    dynamic x;

    if (SC.sym == SC.IDENT) {
        var ident = SC.val;
        x = find(ident);
        SC.getSym();

        if (type(x) == Type) {
            x = Type(x.val);
        }else {
            SC.mark("Not a type");
            x = Type(null);
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
        dynamic y = expression();
        if (SC.sym == SC.RBRAK)
            SC.getSym();
        else SC.mark("']' expected");
        if (SC.sym == SC.OF)
            SC.getSym();
        else
            SC.mark("'of' expected");

        dynamic z = typ().val;

        if (type(x) != Const || x.val < 0) {
            SC.mark('bad lower bound');
            x = Type(null);
        }else if (type(y) != Const || y.val < x.val) {
            SC.mark('bad upper bound');
            x = Type(null);
        }else
            x = Type(CG.genArray(Array(z, x.val, y.val - x.val + 1)));
    }else if (SC.sym == SC.RECORD){
        SC.getSym();
        openScope();
        typedIds(Var);
        while (SC.sym == SC.SEMICOLON) {
            SC.getSym();
            typedIds(Var);
        }
        if (SC.sym == SC.END)
            SC.getSym();
        else
            SC.mark("'end' expected");
        dynamic r = topScope(); closeScope();
        x = Type(CG.genRec(Record(r)));
    } else {
        x = Type(null);
    }

    return x;
}
/*
The function typeIds(kind) parses the production in the structure.

typedIds ::= ident {"," ident} ":" type.

It updates the top scope of symbol table; an error is reported if an identifier is already in the top scope.
The parameter kind is assumed to be callable and applied to the type before an identifier and its type are entered in the symbol table.
*/
dynamic typedIds(dynamic kind){
    dynamic tid =[];
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

    if (SC.sym == SC.COLON) {
        SC.getSym();
        dynamic tp = typ().val;

        if (tp != null) {
            for (String i in tid) {
                if (kind == Var){
                    newDecl(i, Var(tp));
                }else if (kind == Ref){
                    newDecl(i, Ref(tp));
                }else if (kind == Type){
                    newDecl(i, Type(tp));
                }else if (kind == Proc){
                    newDecl(i, Proc(tp));
                }else if (kind == StdProc){
                    newDecl(i, StdProc(tp));
                }else if (kind == Record){
                    newDecl(i, Record(tp));
                }
            }
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
dynamic declarations(dynamic allocVar){
    if (!FIRSTDECL.contains(SC.sym) && !FOLLOWDECL.contains(SC.sym)){
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

            dynamic x = expression();
            if (type(x) == Const)
                newDecl(ident, x);
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
            var ident = SC.val; SC.getSym();
            if (SC.sym == SC.EQ)
                SC.getSym();
            else
                SC.mark("= expected");

            dynamic x = typ();
            newDecl(ident, x);

            if (SC.sym == SC.SEMICOLON)
                SC.getSym();
            else
                SC.mark("; expected");
        }else {
            SC.mark("type name expected");
        }
    }
    start = topScope().length;

    while (SC.sym == SC.VAR) {
        SC.getSym();
        typedIds(Var);
        if (SC.sym == SC.SEMICOLON)
            SC.getSym();
        else
            SC.mark("; expected");
    }
    dynamic varsize = allocVar(topScope(), start);

    while (SC.sym == SC.PROCEDURE){
        SC.getSym();
        if (SC.sym == SC.IDENT)
            SC.getSym();
        else
            SC.mark("procedure name expected");

        String ident = SC.val;
        newDecl(ident, Proc([]));
        dynamic sc = topScope();
        openScope();

        dynamic fp;
        if (SC.sym == SC.LPAREN){
            SC.getSym();
            if ({SC.VAR, SC.IDENT}.contains(SC.sym)) {
                if (SC.sym == SC.VAR) {
                    SC.getSym();
                    typedIds(Ref);
                } else {
                    typedIds(Var);
                }
                while (SC.sym == SC.SEMICOLON) {
                    SC.getSym();
                    if (SC.sym == SC.VAR) {
                        SC.getSym();
                        typedIds(Ref);
                    } else {
                        typedIds(Var);
                    }
                }
            }else {
                SC.mark("formal parameters expected");
            }
            fp = topScope();
            sc[sc.length - 1].par = fp.sublist(0, fp.length);
            if (SC.sym == SC.RPAREN)
                SC.getSym();
            else
                SC.mark(") expected");
        }else
            fp = [];

        dynamic parsize = CG.genProcStart(ident, fp);
        if (SC.sym == SC.SEMICOLON)
            SC.getSym();
        else
            SC.mark("; expected");

        dynamic localsize = declarations(CG.genLocalVars);

        CG.genProcEntry(ident, parsize, localsize);
        dynamic x = compoundStatement(); CG.genProcExit(x, parsize, localsize);
        closeScope();
        if (SC.sym == SC.SEMICOLON)
            SC.getSym();
        else
            SC.mark("; expected");
    }
    return varsize;
}

/*
The function below parses the production

    program ::= "program" ident ";" declarations compoundStatement

If there is no error, then the generated code is returned. The standard identifiers are entered initially in the symbol table.
*/
dynamic program(){

    newDecl("boolean", Type(Bool));
    newDecl("integer", Type(Int));
    newDecl("true", Const(Bool, 1));
    newDecl("false", Const(Bool, 0));
    newDecl("read", StdProc([Ref(Int)]));
    newDecl("write", StdProc([Var(Int)]));
    newDecl("writeln", StdProc([]));

    CG.genProgStart();

    if (SC.sym == SC.PROGRAM)
        SC.getSym();
    else
        SC.mark("'program' expected");

    var ident = SC.val;

    if (SC.sym == SC.IDENT)
        SC.getSym();
    else
        SC.mark("program name expected");

    if (SC.sym == SC.SEMICOLON)
        SC.getSym();
    else
        SC.mark("; expected");

    declarations(CG.genGlobalVars);
    printSymTab();

    CG.genProgEntry(ident);
    dynamic x = compoundStatement();
    return CG.genProgExit(x);
}

/*
This functions compiles the source program that is given bu the variable str.
*/
void compileString(String src){
    SC.init(src);
    init();
    var p = program();
    if (p != null && !SC.error){
        print(p);
    }else{
        print("Cant print due to error occured...printing p");
        //print(p);
    }
}

/*
Helper function that returns the type of an object.
*/
dynamic type(dynamic x){
    return x.runtimeType;
}