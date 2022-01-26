package random

import zio.{Ref, UIO, ZIO, ZLayer}

class RandomBoundedIntFake private (
    values: Ref[Seq[Int]]
) extends RandomBoundedInt:
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int] =
    for
      remainingValues <- values.get
      nextValue <-
        if (remainingValues.isEmpty)
          ZIO.die(
            new Exception(
              "Did not provide enough values!"
            )
          )
        else
          ZIO.succeed(remainingValues.head)
      _ <- values.set(remainingValues.tail)
    yield remainingValues.head
end RandomBoundedIntFake

object RandomBoundedIntFake:
  def apply(
      values: Seq[Int]
  ): ZLayer[Any, Nothing, RandomBoundedInt] =
    (for
      valuesR <- Ref.make(values)
    yield new RandomBoundedIntFake(valuesR)).toLayer
