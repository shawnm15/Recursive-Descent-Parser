import P0, SC, ST
import datetime
def scan_src(src: str):
    SC.init(src)
    yield (SC.sym, SC.val)
    while SC.sym != SC.EOF:
        SC.getSym()
        yield (SC.sym, SC.val)

def test_scanner(src: str):
    print('"%s":' % src, [s for s in scan_src(src)])

# test_scanner('program bitwise;')
# test_scanner('17 & -15')
# test_scanner('-1234 | 12')

def main():
    a = datetime.datetime.now()
    for i in range(0,100):
        P0.compileString("""
program linearsearch;
  const N = 5;
  var a: array [0 .. N - 1] of integer;
  var i, x: integer;
begin
  a[0] := 3; a[1] := 9; a[2] := -4; a[3] := 5; a[4] := 7; {random values}
  i := 0; x := 7;
  while (i < N) and (a[i] <> x) do i := i + 1;
  write(i)
end
        """, target='wat')
    b = datetime.datetime.now()
    delta = b - a
    print("The execution time is:", str(delta.total_seconds() * 1000)+"ms")

main()