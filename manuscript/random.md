# Random

-- Subject Dependencies: Console

TODO All the prose to justify these hoops

```scala
import zio.{Console, Has, UIO, ZIO, ZLayer}
import zio.Runtime.default.unsafeRun
```

```scala
import scala.util.Random
import fakeEnvironmentInstances.FakeConsole

val low = 1
// low: Int = 1
val high = 10
// high: Int = 10

val prompt =
  s"I'm thinking of a number between $low and $high.\n" +
    "Guess: "
// prompt: String = """I'm thinking of a number between 1 and 10.
// Guess: """

val sideEffectingGuessingGame =
  for
    _ <- Console.print(prompt)
    answer = Random.between(low, high)
    guess <- Console.readLine
    response =
      if answer == guess.toInt then
        "You got it!"
      else
        s"BZZ Wrong!! Answer was $answer"
  yield prompt + guess + "\n" + response
// sideEffectingGuessingGame: ZIO[Has[Console], IOException, String] = zio.ZIO$FlatMap@3a2aa688
```

```scala
unsafeRun(
  sideEffectingGuessingGame.provideLayer(
    ZLayer.succeed(FakeConsole.single("3"))
  )
)
// res0: String = """I'm thinking of a number between 1 and 10.
// Guess: 3
// BZZ Wrong!! Answer was 5"""
```

```scala
import fakeEnvironmentInstances.FakeConsole
import fakeEnvironmentInstances.RandomInt

unsafeRun(RandomInt.RandomIntLive.nextInt)
// res1: Int = -1312201061
```

To properly access a `Random` integer, we will construct a small class that implements this operation
in an proper effectful way.

```scala
import zio.Console.printLine

import zio.{Random}

trait RandomIntBetween:
  def nextIntBetween(high: Int, low: Int): UIO[Int]

object RandomIntBetween:
  object RandomIntBetween
      extends RandomIntBetween:

    override def nextIntBetween(
high: Int, low: Int
    ): UIO[Int] =
      ZIO.succeed(scala.util.Random.between(low, high))

class FakeRandomIntBetween(hardcodedValue: Int)
    extends RandomIntBetween:

  override def nextIntBetween(high: Int, low: Int): UIO[Int] =
    UIO.succeed(hardcodedValue)
```

```scala
import scala.util.Random
import fakeEnvironmentInstances.FakeConsole

val effectfulGuessingGame =
  for
    _ <- Console.print(prompt)
    answer <- 
      ZIO
        .accessZIO[Has[RandomIntBetween]](
          _.get.nextIntBetween(high, low)
        )
    guess <- Console.readLine
    response =
      if answer == guess.toInt then
        "You got it!!"
      else
        s"BZZ Wrong!! Answer was $answer"
  yield prompt + guess + "\n" + response
// effectfulGuessingGame: ZIO[Has[RandomIntBetween] & Has[Console], IOException, String] = zio.ZIO$FlatMap@3ee690d3
```

```scala
unsafeRun(
  effectfulGuessingGame.provideLayer(
    ZLayer.succeed(FakeConsole.single("3")) ++ ZLayer.succeed[RandomIntBetween](
      FakeRandomIntBetween(3)
    )
  )
)
// res2: String = """I'm thinking of a number between 1 and 10.
// Guess: 3
// You got it!!"""
```