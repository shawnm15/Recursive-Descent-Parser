from SC import TIMES, DIV, MOD, AND, PLUS, MINUS, OR, EQ, NE, LT, GT, LE, \
     GE, NOT, mark
from ST import indent, Var, Ref, Const, Type, Proc, StdProc, Int, Bool, Array, Record

'''
	Function that initializes global vars. Creates the list that will store the generated code.
'''
def genProgStart():
    global curlev, memsize, asm
    curlev, memsize = 0, 0
    asm = ['(module',
           '(import "P0lib" "write" (func $write (param i32)))',
           '(import "P0lib" "writeln" (func $writeln))',
           '(import "P0lib" "read" (func $read (result i32)))']
'''
Following procedures "generate code" for all P0 types by determining the size of objects and store in the size field.

	Integers and booleans occupy 4 bytes
	The size of a record is the sum of the sizes of its field; the offset of a field is the sum of the size of the preceding fields
	The size of an array is its length times the size of the base type.
	
'''
def genBool(b):
    #     print("shawn-b is ",b)
    #     print("shawn-type b",type(b))
    # b is Bool
    b.size = 1;
    return b

def genInt(i):
    # i is Int
    i.size = 4;
    return i

def genRec(r):
    # r is Record
    s = 0
    for f in r.fields:
        f.offset = s
        s = s + f.tp.size
    r.size = s
    return r

def genArray(a: Array):
    # a is Array
    a.size = a.length * a.base.size
    return a

'''
The symbol table assigns to each entry the level of declaration in the field lev: int. Variables are assigned a name: str field by the symbol table and an adr: int field by the code generator. The use of the lev field is extended:

	lev > 0: local Int, Bool variable or parameter allocated in the procedure (function) call frame, accessed by name,
	lev = 0: global Int, Bool variable allocated as a WebAssembly global variable, accessed by name,
	lev = -1: Int, Bool variable allocated on the expression stack,
	lev = -2: Int, Bool, Array, Record variable allocated in the WebAssembly memory, accessed by adr.

	For each declared global variable, genGlobalVars(sc, start) allocates a global WebAssembly variable by the same name,
	if the type is Int or Bool, or reserves space in the memory, if the type is Array, Record. The parameter sc contains
	the top scope with all declarations parsed so far; only variable declarations from index start on in the top scope are considered.
	
'''
def genGlobalVars(sc, start):
    global memsize
    for i in range(start, len(sc)):
        if type(sc[i]) == Var:
            if sc[i].tp in (Int, Bool):
                asm.append('(global $' + sc[i].name + ' (mut i32) i32.const 0)')
            elif type(sc[i].tp) in (Array, Record):
                sc[i].lev, sc[i].adr, memsize = -2, memsize, memsize + sc[i].tp.size
            else:
                mark('WASM: type?')


def genLocalVars(sc, start):
    for i in range(start, len(sc)):
        if type(sc[i]) == Var:
            if sc[i].tp in (Int, Bool):
                asm.append('(local $' + sc[i].name + ' i32)')
            elif type(sc[i].tp) in (Array, Record):
                mark('WASM: no local arrays, records')
            else:
                mark('WASM: type?')
    return None

'''
Procedure loadItem(x) generates code for loading x on the expression stack, assuming x is global Var,
	local Var, stack Var, memory Var, local Ref, stack Ref, Const.

'''
def loadItem(x):
    if type(x) == Var:
        if x.lev == 0: asm.append('global.get $' + x.name) # global Var
        elif x.lev == curlev: asm.append('local.get $' + x.name) # local Var
        elif x.lev == -2: # memory Var
            asm.append('i32.const ' + str(x.adr))
            asm.append('i32.load')
        elif x.lev != -1: mark('WASM: var level!') # already on stack if lev == -1
    elif type(x) == Ref:
        if x.lev == -1: asm.append('i32.load')
        elif x.lev == curlev:
            asm.append('local.get $' + x.name)
            asm.append('i32.load')
        else: mark('WASM: ref level!')
    elif type(x) == Const: asm.append('i32.const ' + str(x.val))



def genVar(x):
    # x is Var, Ref
    if 0 < x.lev < curlev: mark('WASM: level!')
    if type(x) == Ref:
        y = Ref(x.tp); y.lev, y.name = x.lev, x.name
        # if type(x.tp) in (Array, Record):
        #    if x.lev > 0: y.name = x.name
    elif type(x) == Var:
        y = Var(x.tp); y.lev, y.name = x.lev, x.name
        # if x.lev >= 0: y.name = x.name
        if x.lev == -2: y.adr = x.adr
    return y

'''
	Procedure genConst(x) does not need to generate any code.

'''
def genConst(x):
    # x is Const
    return x


'''
Procedure genUnaryOp(op, x) generates code for op x if op is MINUS, NOT and x is Int, Bool, respectively.
	If op is AND, OR, item x is the first operand and an if instruction is generated.
	
'''
def genUnaryOp(op, x):
    loadItem(x)
    if op == MINUS:
        asm.append('i32.const -1')
        asm.append('i32.mul')
        x = Var(Int); x.lev = -1
    elif op == NOT:
        asm.append('i32.eqz')
        x = Var(Bool); x.lev = -1
    elif op == AND:
        asm.append('if (result i32)')
        x = Var(Bool); x.lev = -1
    elif op == OR:
        asm.append('if (result i32)')
        asm.append('i32.const 1')
        asm.append('else')
        x = Var(Bool); x.lev = -1
    else: mark('WASM: unary operator?')
    return x


'''
Procedure genBinaryOp(op, x, y) generates code for x op y if op is PLUS, MINUS, TIMES, DIV, MOD. If op is AND, OR, code for x and the start of an if
	instruction has already been generated; code for y and the remainder of the if instruction is generated.
'''
def genBinaryOp(op, x, y):
    if op in (PLUS, MINUS, TIMES, DIV, MOD):
        loadItem(x); loadItem(y)
        asm.append('i32.add' if op == PLUS else \
                   'i32.sub' if op == MINUS else \
                   'i32.mul' if op == TIMES else \
                   'i32.div_s' if op == DIV else \
                   'i32.rem_s' if op == MOD else '?')
        x = Var(Int); x.lev = -1
    elif op == AND:
        loadItem(y) # x is already on the stack
        asm.append('else')
        asm.append('i32.const 0')
        asm.append('end')
        x = Var(Bool); x.lev = -1
    elif op == OR:
        loadItem(y) # x is already on the stack
        asm.append('end')
        x = Var(Bool); x.lev = -1
    else: assert False
    return x

'''
	Procedure `genRelation(op, x, y)` generates code for `x op y` if `op` is `EQ`, `NE`, `LT`, `LE`, `GT`, `GE`.

'''
def genRelation(op, x, y):
    loadItem(x); loadItem(y)
    asm.append('i32.eq' if op == EQ else \
               'i32.ne' if op == NE else \
               'i32.lt_s' if op ==  LT else \
               'i32.gt_s' if op == GT else \
               'i32.le_s' if op == LE else \
               'i32.ge_s' if op == GE else '?')
    x = Var(Bool); x.lev = -1
    return x

'''
Procedure genSelect(x, f) generates code for x.f, provided f is in x.fields. If x is Var, i.e. allocated in memory, only x.adr is updated and no code is generated.
	If x is Ref, i.e. a reference to memory, code for adding the offset of f is generated. An updated item is returned.
	
'''
def genSelect(x, f):
    # x.f, assuming x.tp is Record and x is global Var, local Ref, stack Ref
    # and f is Field
    if type(x) == Var: x.adr += f.offset
    elif type(x) == Ref:
        if x.lev > 0: asm.append('local.get $' + x.name)
        asm.append('i32.const ' + str(f.offset))
        asm.append('i32.add')
        x.lev = -1
    x.tp = f.tp
    return x


'''
Procedure genIndex(x, y) generates code for x[y], assuming x is Var or Ref, x.tp is Array, and y.tp is Int.
	If y is Const, only x.adr is updated and no code is generated, otherwise code for array index calculation is generated.
	
'''
def genIndex(x, y):
    # x[y], assuming x.tp is Array and x is global Var, local Ref, stack Ref
    # and y is Const, local Var, global Var, stack Var
    if type(x) == Var: # at x.adr
        if type(y) == Const:
            x.adr += (y.val - x.tp.lower) * x.tp.base.size
            x.tp = x.tp.base
        else: # y is global Var, local Var, stack Var
            loadItem(y) # y on stack
            if x.tp.lower != 0:
                asm.append('i32.const ' + str(x.tp.lower))
                asm.append('i32.sub')
            asm.append('i32.const ' + str(x.tp.base.size))
            asm.append('i32.mul')
            asm.append('i32.const ' + str(x.adr))
            asm.append('i32.add')
            x = Ref(x.tp.base); x.lev = -1
    else: # x is local Ref, stack Ref; y is Const, global Var, local Var, stack Var
        if x.lev == curlev: loadItem(x); x.lev = -1
        if type(y) == Const:
            asm.append('i32.const ' + str((y.val - x.tp.lower) * x.tp.base.size))
            asm.append('i32.add')
        else:
            loadItem(y) # y on stack
            asm.append('i32.const ' + str(x.tp.lower))
            asm.append('i32.sub')
            asm.append('i32.const ' + str(x.tp.base.size))
            asm.append('i32.mul')
            asm.append('i32.add')
        x.tp = x.tp.base
    return x


'''
Procedure `genAssign(x, y)` generates code for `x := y`, provided `x` is `Var`, `Ref` and `y` is `Var`, `Ref`.
	
'''
def genAssign(x, y):
    if type(x) == Var:
        if x.lev == -2: asm.append('i32.const ' + str(x.adr))
        loadItem(y)
        if x.lev == 0:
            asm.append('global.set $' + x.name)
        elif x.lev == curlev: asm.append('local.set $' + x.name)
        elif x.lev == -2: asm.append('i32.store')
        else: mark('WASM: level!')
    elif type(x) == Ref:
        if x.lev == curlev: asm.append('local.get $' + x.name)
        loadItem(y)
        asm.append('i32.store')
    else: assert False



def genProgEntry(ident):
    asm.append('(func $program')

def genProgExit(x):
    asm.append(')\n(memory ' + str(memsize // 2 ** 16 + 1) + ')\n(start $program)\n)')
    return '\n'.join(l for l in asm)

def genProcStart(ident, fp):
    global curlev
    if curlev > 0: mark('WASM: no nested procedures')
    curlev = curlev + 1
    asm.append('(func $' + ident + ' ' + ' '.join('(param $' + e.name + ' i32)' for e in fp) + '')
    for p in fp:
        if p.tp in (Int, Bool) and type(p) == Ref:
            mark('WASM: only array and record reference parameters')
        elif type(p.tp) in (Array, Record) and type(p) == Var:
            mark('WASM: no structured value parameters')

def genProcEntry(ident, parsize, localsize):
    pass

def genProcExit(x, parsize, localsize):
    global curlev
    curlev = curlev - 1
    asm.append(')')

def genActualPara(ap, fp, n):
    if type(fp) == Ref:  # reference parameter, assume ap is Var
        if ap.lev == -2: asm.append('i32.const ' + str(ap.adr))
        # else ap.lev == -1, on stack already
    elif type(ap) in (Var, Ref, Const):
        loadItem(ap)
    else:
        mark('unsupported parameter type')

def genCall(pr, ap):
    asm.append('call $' + pr.name)

def genRead(x):
    asm.append('call $read')
    y = Var(Int);
    y.lev = -1

def genWrite(x):
    loadItem(x)
    asm.append('call $write')

def genWriteln():
    asm.append('call $writeln')

def genSeq(x, y):
    pass

def genThen(x):
    loadItem(x)
    asm.append('if')
    return x

def genIfThen(x, y):
    asm.append('end')

def genElse(x, y):
    asm.append('else')

def genIfElse(x, y, z):
    asm.append('end')

def genWhile():
    asm.append('loop')

def genDo(x):
    loadItem(x)
    asm.append('if')
    return x

def genWhileDo(t, x, y):
    asm.append('br 1')
    asm.append('end')
    asm.append('end')