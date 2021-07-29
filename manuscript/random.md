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

val logic =
  for
    _ <-
      Console.printLine(
        s"I'm thinking of a number between $low and $high"
      )
    answer = Random.between(low, high)
    _ <- Console.print("Guess: ")
    guess <- Console.readLine
  yield
    if answer == guess.toInt then
      "You got it!"
    else
      "BZZ Wrong!"
// logic: ZIO[Has[Console], IOException, String] = zio.ZIO$FlatMap@29c1fefa

val assembledProgram =
  for
    fakeConsole <-
      FakeConsole.withInput(
        "3",
        "5",
        "7",
        "9",
        "11",
        "13"
      )
    result <-
      logic.provideCustomLayer(
        ZLayer.succeed(fakeConsole)
      )
  yield result
// assembledProgram: ZIO[Has[Clock] & Has[Console] & Has[System] & Has[Random], IOException, String] = zio.ZIO$FlatMap@5f24c343
```

```scala
unsafeRun(assembledProgram)
// res0: String = "BZZ Wrong!"
```

```scala
import fakeEnvironmentInstances.FakeConsole
import fakeEnvironmentInstances.RandomInt

unsafeRun(RandomInt.RandomIntLive.nextInt)
// res1: Int = -1420424481
```

```scala
import zio.Console.printLine

import zio.{Random}

trait RandomIntBounded:
  def nextIntBounded(n: Int): UIO[Int]

object RandomIntBounded:
  object RandomIntBoundedLive
      extends RandomIntBounded:

    override def nextIntBounded(
        n: Int
    ): UIO[Int] =
      ZIO.succeed(scala.util.Random.nextInt(n))

class FakeRandomIntBounded(hardcodedValue: Int)
    extends RandomIntBounded:

  override def nextIntBounded(n: Int): UIO[Int] =
    UIO.succeed(hardcodedValue)

def luckyZ(
    i: Int
): ZIO[Has[RandomIntBounded], Nothing, Boolean] =
  ZIO
    .accessZIO[Has[RandomIntBounded]](
      _.get.nextIntBounded(i)
    )
    .map(_ == 0)

val myAppLogic =
  for
    isLucky <- luckyZ(50)
    result =
      if isLucky then
        "You are lucky!"
      else
        "Sorry"
    // TODO Figure out why these don't play
    // nicely with mdoc
    _ <- printLine(result)
    _ <- ZIO.debug(result)
    _ <-
      ZIO.succeed(println("Result: " + result))
  yield ()
// myAppLogic: ZIO[Has[RandomIntBounded] & Has[Console], IOException, Unit] = zio.ZIO$FlatMap@543abc3

unsafeRun(
  myAppLogic.provideCustomLayer(
    ZLayer.succeed[RandomIntBounded](
      FakeRandomIntBounded(0)
    )
  )
)
// Result: You are lucky!
```