package interpreter.level2_chaining

case class ToyRandom(
    nextAction: String => Operation
) extends Operation

val program: Operation =
  ToyRandom(
    s => Print(s)
  )

def interpreter(doSomething: Operation): Unit =
  doSomething match
    case DoNothing =>
      ()
    case r: ToyRandom =>
      val i =
        scala.util.Random.nextInt()
      interpreter(r.nextAction(i.toString))
    case p: Print =>
      println(p.s)
      interpreter(p.nextAction(""))

@main
def m4 =
  interpreter(program)
