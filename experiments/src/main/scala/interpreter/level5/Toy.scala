package interpreter.level5

import environment_exploration.ToyEnvironment

sealed trait DoSomething:
  val toDo: (s: String) => DoSomething

case class DoNothing() extends DoSomething:
  override val toDo = s => DoNothing()

case class Print(s: String)(myTodo: String => DoSomething = _ => DoNothing()) extends DoSomething:
  override val toDo = myTodo

case class ToyRandom()(myTodo: String => DoSomething) extends DoSomething:
  override val toDo = myTodo

val program: DoSomething = ToyRandom()(s => Print(s)())

def interpreter(env: ToyEnvironment[scala.util.Random & scala.Console.type])(doSomething: DoSomething): Unit =
  doSomething match
    case _: DoNothing =>
      ()
    case r: ToyRandom =>
      val i = env.get[scala.util.Random].nextInt()
      interpreter(env)(r.toDo(i.toString))
    case p: Print =>
      env.get[scala.Console.type].println(p.s)
      interpreter(env)(p.toDo(""))

@main
def m1 =
  val env = ToyEnvironment(Map.empty)
    .add[scala.util.Random](scala.util.Random)
    .add(scala.Console)

  interpreter(env)(program)
