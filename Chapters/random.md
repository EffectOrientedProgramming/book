# Random

TODO All the prose to justify these hoops

```scala mdoc
import fakeEnvironmentInstances.FakeConsole
import fakeEnvironmentInstances.RandomInt
import zio.Runtime.default.unsafeRun

unsafeRun(RandomInt.RandomIntLive.nextInt)
```

```scala mdoc
import zio.{
  BuildFrom,
  Chunk,
  Console,
  Has,
  Random,
  UIO,
  ZIO,
  ZLayer
}
import zio.Console.printLine

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

unsafeRun(
  myAppLogic.provideCustomLayer(
    ZLayer.succeed[RandomIntBounded](
      FakeRandomIntBounded(0)
    )
  )
)
```