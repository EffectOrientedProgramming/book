package effects

import zio.{
  BuildFrom,
  Chunk,
  Console,
  Has,
  Random,
  UIO,
  ZIO,
  ZServiceBuilder
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
    val myRandom: ZServiceBuilder[Any, Nothing, Has[
      RandomIntBounded
    ]] = ZServiceBuilder.succeed(FakeRandomIntBounded(0))

    myAppLogic
      .provideServices(myRandom)
      // does not work for some reason
      // .injectSome[Has[Console]](myRandom)
      .exitCode

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
  def intBetween(low: Int, high: Int): UIO[Int]

object RandomIntBetween:
  object RandomIntBetween
      extends RandomIntBetween:
    override def intBetween(
        low: Int,
        high: Int
    ): UIO[Int] =
      ZIO.succeed(
        scala.util.Random.between(low, high)
      )

class FakeRandomIntBetween(hardcodedValue: Int)
    extends RandomIntBetween:
  override def intBetween(
      low: Int,
      high: Int
  ): UIO[Int] = UIO.succeed(hardcodedValue)

def effectfulIntBetween(low: Int, high: Int) =
  ZIO.accessZIO[RandomIntBetween](
    _.intBetween(high, low)
  )

@main
def demostuff(): Unit =

  def foo(arg: => Int): Int =
    val x = arg
    println("In foo")
    x

  foo {
    println("hi first")
    10
  }

  def foo2(arg: () => Int): Int =
    arg()
    println("In foo2")
    arg()

  foo2 { () =>
    println("hi")
    10
  }
end demostuff
