package fakeEnvironmentInstances

import zio.{
  BuildFrom,
  Chunk,
  Console,
  Has,
  Layer,
  Random,
  UIO,
  ZIO,
  ZLayer,
  ZTraceElement
}
import zio.Console.printLine

trait RandomInt:
  def nextIntBounded(n: Int): UIO[Int]
  def nextInt: UIO[Int]
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int]

class FakeRandomInt(hardcodedValue: Int)
    extends RandomInt:
  override def nextIntBounded(n: Int): UIO[Int] =
    UIO.succeed(hardcodedValue)

  override def nextInt: UIO[Int] =
    UIO.succeed(hardcodedValue)
  override def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int] = UIO.succeed(hardcodedValue)

object RandomInt:
  object RandomIntLive extends RandomInt:
    // Consider whether to re-implement from
    // scratch
    def nextIntBounded(n: Int): UIO[Int] =
      Random.RandomLive.nextIntBounded(n)

    def nextInt: UIO[Int] =
      Random.RandomLive.nextInt
    def nextIntBetween(
        minInclusive: Int,
        maxExclusive: Int
    ): UIO[Int] =
      Random
        .RandomLive
        .nextIntBetween(
          minInclusive,
          maxExclusive
        )

  val live: Layer[Nothing, Has[RandomInt]] =
    ZLayer.succeed(RandomIntLive)
end RandomInt

class FakeRandom(i: Int) extends Random:
  def nextBoolean(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Boolean] = ???
  def nextBytes(length: => Int)(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[zio.Chunk[Byte]] = ???
  def nextDouble(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Double] = ???
  def nextDoubleBetween(
      minInclusive: => Double,
      maxExclusive: => Double
  )(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Double] = ???
  def nextFloat(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Float] = ???
  def nextFloatBetween(
      minInclusive: => Float,
      maxExclusive: => Float
  )(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Float] = ???
  def nextGaussian(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Double] = ???
  def nextInt(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Int] = ???
  def nextIntBetween(
      minInclusive: => Int,
      maxExclusive: => Int
  )(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Int] = ???
  def nextIntBounded(n: => Int)(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Int] = ???
  def nextLong(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Long] = ???
  def nextLongBetween(
      minInclusive: => Long,
      maxExclusive: => Long
  )(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Long] = ???
  def nextLongBounded(n: => Long)(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Long] = ???
  def nextPrintableChar(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Char] = ???
  def nextString(length: => Int)(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[String] = ???
  def setSeed(seed: => Long)(implicit
      trace: zio.ZTraceElement
  ): zio.UIO[Unit] = ???
  def shuffle[A, Collection[+Element]
    <: Iterable[Element]](
      collection: => Collection[A]
  )(implicit
      bf: BuildFrom[Collection[A], A, Collection[
        A
      ]],
      trace: ZTraceElement
  ): UIO[Collection[A]] = ???
/* override def nextIntBounded( n: => Int ):
 * UIO[Int] = UIO.succeed(i) override def
 * nextBoolean: UIO[Boolean] = ???
 * override def nextBytes( length: => Int ):
 * UIO[Chunk[Byte]] = ???
 * override def nextDouble: UIO[Double] = ???
 * override def nextDoubleBetween( minInclusive:
 * => Double, maxExclusive: => Double ):
 * UIO[Double] = ???
 * override def nextFloat: UIO[Float] = ???
 * override def nextFloatBetween( minInclusive:
 * => Float, maxExclusive: => Float ): UIO[Float]
 * = ???
 * override def nextGaussian: UIO[Double] = ???
 * override def nextInt: UIO[Int] = ???
 * override def nextIntBetween( minInclusive: =>
 * Int, maxExclusive: => Int ): UIO[Int] = ???
 * override def nextLong: UIO[Long] = ???
 * override def nextLongBetween( minInclusive: =>
 * Long, maxExclusive: => Long ): UIO[Long] = ???
 * override def nextLongBounded( n: => Long ):
 * UIO[Long] = ???
 * override def nextPrintableChar: UIO[Char] =
 * ???
 * override def nextString( length: => Int ):
 * UIO[String] = ???
 * override def setSeed( seed: => Long ):
 * UIO[Unit] = ???
 * def shuffle[A, Collection[+Element] <:
 * Iterable[Element]]( collection: =>
 * Collection[A] )(implicit bf:
 * BuildFrom[Collection[A], A, Collection[ A ]]
 * ): UIO[Collection[A]] = ??? */
end FakeRandom
