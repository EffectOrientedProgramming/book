## interpreter-level2_chaining
 Newer stuff!
 

### experiments/src/main/scala/interpreter/level2_chaining/3_chainedOperations.scala
```scala
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

```


### experiments/src/main/scala/interpreter/level2_chaining/4_chainedRandom.scala
```scala
package interpreter.level2_chaining

case class ToyRandom(
    nextAction: String => Operation
) extends Operation

val program: Operation = ToyRandom(s => Print(s))

def interpreter(doSomething: Operation): Unit =
  doSomething match
    case DoNothing =>
      ()
    case r: ToyRandom =>
      val i = scala.util.Random.nextInt()
      interpreter(r.nextAction(i.toString))
    case p: Print =>
      println(p.s)
      interpreter(p.nextAction(""))

@main
def m4 = interpreter(program)

```


### experiments/src/main/scala/interpreter/level2_chaining/5_useEnvironmentInstances.scala
```scala
package interpreter.level2_chaining

import environment_exploration.ToyEnvironment

def interpret(
    env: ToyEnvironment[
      scala.util.Random & scala.Console.type
    ],
    doSomething: Operation
): Unit =
  doSomething match
    case DoNothing =>
      ()
    case r: ToyRandom =>
      val i =
        env.get[scala.util.Random].nextInt()
      interpret(env, r.nextAction(i.toString))
    case p: Print =>
      env.get[scala.Console.type].println(p.s)
      interpret(env, p.nextAction(""))

@main
def demoInterpretWithEnvironment() =
  val env =
    ToyEnvironment(Map.empty)
      .add[scala.util.Random](scala.util.Random)
      .add(scala.Console)

  interpret(env, program)

```


