package interpreter.level3

trait DoSomething:
  val toDo: (s: String) => DoSomething

case class DoNothing() extends DoSomething:
  override val toDo = s => DoNothing()

case class Print(s: String)(myTodo: String => DoSomething = _ => DoNothing()) extends DoSomething:
  override val toDo = myTodo

val something: String => DoSomething = s =>
  if s == "hello" then Print(s)() else DoNothing()

val program: DoSomething = Print("asdf")(something)

def interpreter(doSomething: DoSomething): Unit =
  doSomething match
    case _: DoNothing => ()
    case p: Print =>
      println(p.s)
      interpreter(p.toDo("hello"))

@main
def m1 =
  interpreter(Print("some thing")())
  interpreter(program)
