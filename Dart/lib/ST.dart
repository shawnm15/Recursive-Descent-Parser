library ST;

import 'package:DartParser/SC.dart' show mark;

var symTab;

String indent(int n){
    return '  ' + n.toString();
}

class Var{

    String name;
    var lev;
    dynamic tp;
    var size;
    var offset;
    var adr;

    Var(dynamic tp){
        this.tp = tp;
    }

    @override
    String toString() {
        return "Var(name = ${this.name}, lev = ${this.lev} , tp = ${this.tp} )";
    }
}

class Ref{

    String name;
    var lev;
    dynamic tp;
    var size;
    var offset;
    var adr;

    Ref(dynamic tp){
        this.tp = tp;
    }

    @override
    String toString() {
        return "Ref(name = ${this.name}, lev = ${this.lev} , tp = ${this.tp} )";
    }
}

class Const{

    String name;
    dynamic val;
    dynamic tp;
    int size;
    int offset;
    var adr;

    Const(dynamic tp, dynamic val){
        this.tp = tp;
        this.val = val;
    }

    @override
    String toString() {
        return "Const(name = ${this.name}, tp = ${this.tp}, val = ${this.val})";
    }
}

class Type{

    String name;
    dynamic val;
    dynamic tp;
    var size;
    var offset;
    var adr;

    Type(dynamic tp){
        this.tp = null;
        this.val = tp;
    }

    @override
    String toString() {
        return "Type(name = ${this.name}, val = ${this.val})";
    }
}

class Proc{

    String name;
    int lev;
    dynamic tp;
    dynamic par;
    var adr;

    Proc(dynamic par){
        this.tp = null;
        this.par = par;
    }

    @override
    String toString() {
        return "Proc(name = ${this.name}, lev = ${this.lev}), par = ${this.par.toString()})";
    }
}

class StdProc{

    String name;
    int lev;
    dynamic tp;
    dynamic par;
    var adr;

    StdProc(dynamic par){
        this.tp = null;
        this.par = par;
    }

    @override
    String toString() {
        return "StdProc(name = ${this.name}, lev = ${this.lev}), par = ${this.par.toString()})";
    }
}

class Int{}

class Bool{}

class Enum{}

class Record{

    dynamic fields;
    var offset;
    var size;
    var adr;

    Record(dynamic fields){
        this.fields = fields;
    }

    @override
    String toString() {
        return "Record(fields = ${this.fields.toString()})";
    }
}

class Array{

    dynamic base;
    dynamic lower;
    var length;
    var size;
    var offset;
    var adr;

    Array(dynamic base, var lower, var length){
        this.base = base;
        this.lower = lower;
        this.length = length;
    }

    @override
    String toString() {
        return "Array(lower = ${this.lower}, length = ${this.length}, base = ${this.base} )";
    }
}

/*

*/
void init() {
    symTab = [[]];
}

/*

*/
void printSymTab(){
    for (var l in symTab){
        for (var e in l){
            print(e);
        }
        print("");
    }
}

/*

*/
void newDecl(String name, dynamic entry){
    var top = symTab[0];
    try{
        entry.lev = symTab.length - 1;
    }on NoSuchMethodError{
        //do nothing
    }
    entry.name = name;
    for (var e in top) {
        if (e.name == name) {
            mark("multiple definition");
            return;
        }
    }

    top.add(entry);
}

/*

*/
dynamic find(name){
    for (var l in symTab){
        for (var e in l){
            if (name == e.name){
                return e;
            }
        }
    }
    mark('undefined identifier ' + name);
    return Const(null, 0);

}
/*

*/
void openScope(){
    symTab.insert(0, []);
}
/*

*/
dynamic topScope(){
    return symTab[0];
}
/*

*/
dynamic closeScope(){
    symTab.removeRange(0,1);
}


