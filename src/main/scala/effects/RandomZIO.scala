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

def luckyZ(
    i: Int
): ZIO[Has[Random], Nothing, Boolean] =
  ZIO
    .accessZIO[Has[Random]](
      _.get.nextIntBounded(i)
    )
    .map(_ == 0)

class MyRandom(i: Int) extends Random:
  override def nextIntBounded(n: Int): UIO[Int] =
    UIO.succeed(i)
  override def nextBoolean: UIO[Boolean] = ???
  override def nextBytes(
      length: Int
  ): UIO[Chunk[Byte]] = ???
  override def nextDouble: UIO[Double] = ???
  override def nextDoubleBetween(
      minInclusive: Double,
      maxExclusive: Double
  ): UIO[Double] = ???
  override def nextFloat: UIO[Float] = ???
  override def nextFloatBetween(
      minInclusive: Float,
      maxExclusive: Float
  ): UIO[Float] = ???
  override def nextGaussian: UIO[Double] = ???
  override def nextInt: UIO[Int] = ???
  override def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int] = ???
  override def nextLong: UIO[Long] = ???
  override def nextLongBetween(
      minInclusive: Long,
      maxExclusive: Long
  ): UIO[Long] = ???
  override def nextLongBounded(
      n: Long
  ): UIO[Long] = ???
  override def nextPrintableChar: UIO[Char] = ???
  override def nextString(
      length: Int
  ): UIO[String] = ???
  override def setSeed(seed: Long): UIO[Unit] =
    ???
  def shuffle[A, Collection[
      +Element
  ] <: Iterable[Element]](
      collection: Collection[A]
  )(implicit
      bf: BuildFrom[Collection[A], A, Collection[
        A
      ]]
  ): UIO[Collection[A]] = ???
end MyRandom

object LuckyZ extends zio.App:

  def run(args: List[String]) =
    val myRandom
        : ZLayer[Any, Nothing, Has[Random]] =
      ZLayer.succeed(MyRandom(0))

    myAppLogic
      .provideSomeLayer[Has[Console]](myRandom)
      // does not work for some reason
      // .injectSome[Has[Console]](myRandom)
      .exitCode
  end run

  val myAppLogic =
    for
      isLucky <- luckyZ(50)
      result =
        if isLucky then
          "You are lucky!"
        else
          "Sorry"
      _ <- printLine(result)
    yield ()
end LuckyZ
