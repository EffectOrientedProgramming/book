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
