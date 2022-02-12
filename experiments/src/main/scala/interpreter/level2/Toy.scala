package interpreter.level2

case class Print(s: String)

val program: Seq[Print] =
  Seq(Print("asdf"), Print("hello"))

def interpreter(prints: Seq[Print]): Unit =
  prints match
    case one :: Nil =>
      println(one.s)
    case head :: tail =>
      println(head.s)
      interpreter(tail)

@main
def m1 =
  interpreter(Seq(Print("some thing")))
  interpreter(program)
