package random

import zio.{
  BuildFrom,
  Chunk,
  Console,
  Random,
  UIO,
  ZIO,
  ZLayer,
  Trace
}
import zio.Console.printLine

import java.util.UUID

class RandomZIOFake(i: Int) extends Random:
  def nextUUID(implicit
      trace: Trace
  ): UIO[UUID] = ???
  def nextBoolean(implicit
      trace: zio.Trace
  ): zio.UIO[Boolean] = ???
  def nextBytes(length: => Int)(implicit
      trace: zio.Trace
  ): zio.UIO[zio.Chunk[Byte]] = ???
  def nextDouble(implicit
      trace: zio.Trace
  ): zio.UIO[Double] = ???
  def nextDoubleBetween(
      minInclusive: => Double,
      maxExclusive: => Double
  )(implicit
      trace: zio.Trace
  ): zio.UIO[Double] = ???
  def nextFloat(implicit
      trace: zio.Trace
  ): zio.UIO[Float] = ???
  def nextFloatBetween(
      minInclusive: => Float,
      maxExclusive: => Float
  )(implicit
      trace: zio.Trace
  ): zio.UIO[Float] = ???
  def nextGaussian(implicit
      trace: zio.Trace
  ): zio.UIO[Double] = ???
  def nextInt(implicit
      trace: zio.Trace
  ): zio.UIO[Int] = ???
  def nextIntBetween(
      minInclusive: => Int,
      maxExclusive: => Int
  )(implicit
      trace: zio.Trace
  ): zio.UIO[Int] = ???
  def nextIntBounded(n: => Int)(implicit
      trace: zio.Trace
  ): zio.UIO[Int] = ???
  def nextLong(implicit
      trace: zio.Trace
  ): zio.UIO[Long] = ???
  def nextLongBetween(
      minInclusive: => Long,
      maxExclusive: => Long
  )(implicit
      trace: zio.Trace
  ): zio.UIO[Long] = ???
  def nextLongBounded(n: => Long)(implicit
      trace: zio.Trace
  ): zio.UIO[Long] = ???
  def nextPrintableChar(implicit
      trace: zio.Trace
  ): zio.UIO[Char] = ???
  def nextString(length: => Int)(implicit
      trace: zio.Trace
  ): zio.UIO[String] = ???
  def setSeed(seed: => Long)(implicit
      trace: zio.Trace
  ): zio.UIO[Unit] = ???
  def shuffle[A, Collection[+Element]
    <: Iterable[Element]](
      collection: => Collection[A]
  )(implicit
      bf: BuildFrom[Collection[A], A, Collection[
        A
      ]],
      trace: Trace
  ): UIO[Collection[A]] = ???

end RandomZIOFake
