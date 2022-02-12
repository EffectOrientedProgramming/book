package interpreter.level1

/*
Programs with no chained operations.
The interpreter only handles known types.
*/

case class Print(s: String)

case class Random(f: Int => Unit)

val p1 = Print("hello")
val r1 = Random(println)

def interpreter(pOrR: Print | Random): Unit =
  pOrR match
    case p: Print => println(p.s)
    case r: Random => r.f(scala.util.Random.nextInt())

@main
def m1 =
  interpreter(p1)
  interpreter(r1)
