package interpreter.level1_nochaining

def interpretSequence(prints: Seq[Print]): Unit =
  prints match
    case Nil =>
      ()
    case head :: tail =>
      println(head.s)
      interpretSequence(tail)

@main
def demoSequence =
  val program = Seq(Print("asdf"), Print("hello"))
  interpretSequence(program)
