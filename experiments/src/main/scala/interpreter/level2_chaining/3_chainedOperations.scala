package interpreter.level2_chaining

trait Operation:
  val nextAction: String => Operation

object DoNothing extends Operation:
  override val nextAction = s => DoNothing

case class Print(
    s: String,
    nextAction: String => Operation =
      _ => DoNothing
) extends Operation

//case class Pure(
//    value: String
//) extends Operation

def interpreter3(doSomething: Operation): Unit =
  doSomething match
    case DoNothing =>
      ()
    case Print(s, nextAction) =>
      println(s)
      // It's weird that we're giving this value
      // during interpretation
      interpreter3(nextAction("hello"))

@main
def m1 =
  val conditionallyExecuteAnotherOperation
      : String => Operation =
    s =>
      if s == "hello" then
        Print(s)
      else
        DoNothing

  val program3: Operation =
    Print(
      "asdf",
      conditionallyExecuteAnotherOperation
    )
  interpreter3(Print("some thing"))
  interpreter3(program3)
