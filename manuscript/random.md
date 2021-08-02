# Random

-- Subject Dependencies: `Console`, `ZIO.serviceWith`

TODO All the prose to justify these hoops

```scala
import zio.{Console, Has, UIO, ZIO, ZLayer}
import zio.Runtime.default.unsafeRun
import fakeEnvironmentInstances.FakeConsole
```

```scala
import scala.util.Random

val low = 1
val high = 10

val prompt =
  s"Pick a number between $low and $high: "

// TODO Determine how to handle .toInt failure
// possibility
def checkAnswer(
    answer: Int,
    guess: String
): String =
  if answer == guess.toInt then
    "You got it!"
  else
    s"BZZ Wrong!! Answer was $answer"

val sideEffectingGuessingGame =
  for
    _ <- Console.print(prompt)
    answer = Random.between(low, high)
    guess <- Console.readLine
    response = checkAnswer(answer, guess)
  yield prompt + guess + "\n" + response
```

```scala
unsafeRun(
  sideEffectingGuessingGame.provideLayer(
    ZLayer.succeed(FakeConsole.single("3"))
  )
)
// res0: String = """Pick a number between 1 and 10: 3
// BZZ Wrong!! Answer was 7"""
```

To properly access a `Random` integer, we will construct a small class that implements this operation
in an proper effectful way.

```scala
import zio.Console.printLine

import zio.{Random}

trait RandomInt:
  def between(high: Int, low: Int): UIO[Int]

object RandomInt:
  def between(
      low: Int,
      high: Int
  ): ZIO[Has[RandomInt], Nothing, Int] =
    // TODO Study and determine how/when to
    // introduct `serviceWith`
    ZIO.serviceWith(_.between(high, low))

  object LiveRandomIntBetween extends RandomInt:

    override def between(
        high: Int,
        low: Int
    ): UIO[Int] =
      ZIO.succeed(
        scala.util.Random.between(low, high)
      )
  end LiveRandomIntBetween
end RandomInt

class FakeRandomInt(hardcodedValue: Int)
    extends RandomInt:

  override def between(
      high: Int,
      low: Int
  ): UIO[Int] = UIO.succeed(hardcodedValue)
```

```scala
val effectfulGuessingGame =
  for
    _ <- Console.print(prompt)
    answer <- RandomInt.between(low, high)
    guess <- Console.readLine
    response = checkAnswer(answer, guess)
  yield prompt + guess + "\n" + response
// effectfulGuessingGame: ZIO[Has[RandomInt] & Has[Console], IOException, String] = zio.ZIO$FlatMap@760b77fa
```

```scala
unsafeRun(
  effectfulGuessingGame.provideLayer(
    ZLayer.succeed(FakeConsole.single("3")) ++
      ZLayer.succeed[RandomInt](FakeRandomInt(3))
  )
)
// res1: String = """Pick a number between 1 and 10: 3
// You got it!"""
```