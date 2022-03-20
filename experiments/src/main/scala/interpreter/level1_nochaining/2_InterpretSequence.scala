package interpreter.level1_nochaining

def interpretSequence(
    prints: Seq[Operation]
): Unit =
  prints match
    case Nil =>
      ()
    case head :: tail =>
      interpret(head)
      interpretSequence(tail)

@main
def demoSequence =
  val program =
    Seq(
      Print("asdf"),
      Print("hello"),
      Random(println)
    )

  interpretSequence(program)
