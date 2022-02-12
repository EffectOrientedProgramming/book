package interpreter.level2_chaining

trait DoSomething:
  val nextAction: String => DoSomething

object DoNothing extends DoSomething:
  override val nextAction = s => DoNothing

case class Print(s: String, nextAction: String => DoSomething = _ => DoNothing) extends DoSomething

def interpreter3(doSomething: DoSomething): Unit =
  doSomething match
    case DoNothing => ()
    case Print(s, nextAction) =>
      println(s)
      // It's weird that we're giving this value during interpretation
      interpreter3(nextAction("hello"))

@main
def m1 =
  val conditionallyExecuteAnotherOperation: String => DoSomething = s =>
    if s == "hello" then Print(s) else DoNothing

  val program3: DoSomething = Print("asdf", conditionallyExecuteAnotherOperation)
  interpreter3(Print("some thing"))
  interpreter3(program3)
