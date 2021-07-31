package effects

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

object LuckyZ extends zio.App:
  def run(args: List[String]) =
    val myRandom: ZLayer[Any, Nothing, Has[
      RandomIntBounded
    ]] = ZLayer.succeed(FakeRandomIntBounded(0))

    myAppLogic
      .provideCustomLayer(myRandom)
      // does not work for some reason
      // .injectSome[Has[Console]](myRandom)
      .exitCode
  end run

  val myAppLogic =
    for
      isLucky <- luckyZ(50)
    yield
      if isLucky then
        "You are lucky!"
      else
        "Sorry"
end LuckyZ

trait RandomIntBetween:
  def nextIntBetween(n: Int): UIO[Int]

object RandomIntBetween:
  object RandomIntBetween
    extends RandomIntBetween:
    override def nextIntBetween(
                                 n: Int
                               ): UIO[Int] =
      ZIO.succeed(scala.util.Random.between(n))

class FakeRandomIntBetween(hardcodedValue: Int)
  extends RandomIntBetween:
  override def nextIntBetween(n: Int): UIO[Int] =
    UIO.succeed(hardcodedValue)

def luckyZ(
            i: Int
          ): ZIO[Has[RandomIntBetween], Nothing, Boolean] =
  ZIO
    .accessZIO[Has[RandomIntBetween]](
      _.get.nextIntBetween(i)
    )
    .map(_ == 0)

object LuckyZ extends zio.App:
  def run(args: List[String]) =
    val myRandom: ZLayer[Any, Nothing, Has[
      RandomIntBetween
    ]] = ZLayer.succeed(FakeRandomIntBetween(0))

    myAppLogic
      .provideCustomLayer(myRandom)
      // does not work for some reason
      // .injectSome[Has[Console]](myRandom)
      .exitCode
  end run

  val myAppLogic =
    for
      isLucky <- luckyZ(50)
    yield
      if isLucky then
        "You are lucky!"
      else
        "Sorry"
end LuckyZ
