library SC;

/*
Thwe symbols are encoded as integer constants.
*/
var TIMES = 1; var DIV = 2; var MOD = 3; var AND = 4; var PLUS = 5; var MINUS = 6;var OR = 7; var EQ = 8;
var NE = 9; var LT = 10; var GT = 11; var LE = 12; var GE = 13;var PERIOD = 14; var COMMA = 15; var COLON = 16;
var RPAREN = 17; var RBRAK = 18;var OF = 19; var THEN = 20; var DO = 21; var LPAREN = 22; var LBRAK = 23;
var NOT = 24;var BECOMES = 25; var NUMBER = 26; var IDENT = 27; var SEMICOLON = 28;var END = 29; var ELSE = 30;
var IF = 31; var WHILE = 32; var ARRAY = 33;var RECORD = 34; var CONST = 35; var TYPE = 36; var VAR = 37;
var PROCEDURE = 38;var BEGIN = 39; var PROGRAM = 40; var EOF = 41;

/*
Following variables determine the state of the scanner:

(line, pos) is the location of the current symbol in source
(lastline, lastpos) is used to more accurately report errors
(errline, errpos) is used to suppress multiple errors at the same location
ch is the current character
sym the current symbol
if sym is NUMBER, val is the value of the number
if sym is IDENT, val is the identifier string
source is the string with the source program
*/
var ch, line, lastline, errline, pos, lastpos, errpos, sym, index;
bool error;
String source, src;
dynamic val;

/*

*/
void init(src){
    line = lastline = errline = 1;
    pos = lastpos = errpos = 0;
    sym = null;
    val = null;
    error=false;
    source=src;
    index=0;
    getChar();
    getSym();

}
/*

*/
void getChar(){
    if (index == (source.length)){
        ch = 0;
    }
    else {
        //TODO: probably a bug
        ch = source[index];
        index=index+1;
        lastpos=pos;
        if (ch is String){
            ch = ch.codeUnitAt(0);
        }
        if (ch == '\n'.codeUnitAt(0)){
            pos=0;
            line=line+1;
        }
        else{
            lastline=line;
            pos=pos+1;
        }
    }
}

/*

*/
void mark(String msg){
    if ((lastline > errline) || (lastpos > errpos)){
        print('error: line $lastline pos $lastpos $msg');
    }
    errline=lastline;
    errpos=lastpos;
    error=true;
}

/*

*/
void number(){
    sym=NUMBER;
    val=0;
    if (ch is String){
        ch = ch.codeUnitAt(0);
    }
    while(('0'.codeUnitAt(0) <= ch && ch <= '9'.codeUnitAt(0))){
        val = 10 * val + int.parse(String.fromCharCode(ch));
        getChar();
    }
    if (val >= expo(2,31)){
        mark('number too large');
        val=0;
    }
}
/*

*/
var KEYWORDS = {'div': DIV, 'mod': MOD, 'and': AND, 'or': OR, 'of': OF, 'then': THEN,'do': DO,
    'not': NOT, 'end': END, 'else': ELSE, 'if': IF, 'while': WHILE,'array': ARRAY, 'record': RECORD,
    'const': CONST, 'type': TYPE,'var': VAR, 'procedure': PROCEDURE, 'begin': BEGIN, 'program': PROGRAM};
/*

*/
void identKW(){
    var start=index-1;
    if (ch is String){
        ch = ch.codeUnitAt(0);
    }
    while (('A'.codeUnitAt(0) <= ch && ch <= 'Z'.codeUnitAt(0)) ||
        ('a'.codeUnitAt(0) <= ch && ch <= 'z'.codeUnitAt(0)) ||
        ('0'.codeUnitAt(0) <= ch && ch <= '9'.codeUnitAt(0))){
        getChar();
    }
    val=source.substring(start,index-1);
    if(KEYWORDS.containsKey(val)){
        sym=KEYWORDS[val];
    }
    else{
        sym=IDENT;
    }
}
/*

*/
void comment(){
    while (0 != ch && ch != '}'.codeUnitAt(0)){
        getChar();
    }
    if (ch==0){
        mark('Comment not terminated');
    }
    else{
        getChar();
    }

}
/*

*/
void getSym(){
    if (ch is String){
        ch = ch.codeUnitAt(0);
    }
    while (0 < ch && ch <= ' '.codeUnitAt(0)){
        getChar();
    }
    if (('A'.codeUnitAt(0) <= ch && ch <= 'Z'.codeUnitAt(0)) ||
        ('a'.codeUnitAt(0) <= ch && ch <= 'z'.codeUnitAt(0))) {
        identKW();
    }else if ('0'.codeUnitAt(0) <= ch && ch <= '9'.codeUnitAt(0)) {
        number();
    }else if (ch == '{'.codeUnitAt(0)) {
        comment();
        getSym();
    }else if (ch == '*'.codeUnitAt(0)) {
        getChar();
        sym = TIMES;
    }else if (ch == '+'.codeUnitAt(0)) {
        getChar();
        sym = PLUS;
    }else if (ch == '-'.codeUnitAt(0)) {
        getChar();
        sym = MINUS;
    }else if (ch == '='.codeUnitAt(0)) {
        getChar();
        sym = EQ;
    }else if (ch == '<'.codeUnitAt(0)) {
        getChar();
        if (ch == '='.codeUnitAt(0)) {
            getChar();
            sym = LE;
        }else if (ch == '>'.codeUnitAt(0)) {
            getChar();
            sym = NE;
        }else
            sym = LT;
    }else if (ch == '>'.codeUnitAt(0)) {
        getChar();
        if (ch == '='.codeUnitAt(0)) {
            getChar(); sym = GE;
        }else
            sym = GT;
    }else if (ch == ';'.codeUnitAt(0)) {
        getChar();
        sym = SEMICOLON;
    }else if (ch == ','.codeUnitAt(0)) {
        getChar();
        sym = COMMA;
    }else if (ch == ':'.codeUnitAt(0)) {
        getChar();
        if (ch == '='.codeUnitAt(0)) {
            getChar(); sym = BECOMES;
        }else
            sym = COLON;
    }else if (ch == '.'.codeUnitAt(0)) {
        getChar(); sym = PERIOD;
    }else if (ch == '('.codeUnitAt(0)) {
        getChar(); sym = LPAREN;
    }else if (ch == ')'.codeUnitAt(0)) {
        getChar(); sym = RPAREN;
    }else if (ch == '['.codeUnitAt(0)) {
        getChar(); sym = LBRAK;
    }else if (ch == ']'.codeUnitAt(0)) {
        getChar(); sym = RBRAK;
    }else if (ch == 0) {
        sym = EOF;
    }else {
        mark("illegal character"); getChar(); sym = 0;
    }
}

/*

*/
expo(x,y){
    var res=1;
    for (var i=0;i<y; i++){
        res*=x;
    }
    return res;
}
