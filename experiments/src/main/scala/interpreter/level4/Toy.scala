package interpreter.level4

sealed trait DoSomething:
  val toDo: (s: String) => DoSomething

case class DoNothing() extends DoSomething:
  override val toDo = s => DoNothing()

case class Print(s: String)(
    myTodo: String => DoSomething =
      _ => DoNothing()
) extends DoSomething:
  override val toDo = myTodo

case class Random()(
    myTodo: String => DoSomething
) extends DoSomething:
  override val toDo = myTodo

val program: DoSomething =
  Random()(s => Print(s)())

def interpreter(doSomething: DoSomething): Unit =
  doSomething match
    case _: DoNothing =>
      ()
    case r: Random =>
      val i = scala.util.Random.nextInt()
      interpreter(r.toDo(i.toString))
    case p: Print =>
      println(p.s)
      interpreter(p.toDo(""))

@main
def m1 = interpreter(program)
