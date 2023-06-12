package random

import zio.{Ref, UIO, ZIO, ZLayer}
import zio.direct.*

class RandomBoundedIntFake private (
    values: Ref[Seq[Int]]
) extends RandomBoundedInt:
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int] =
    defer {
      val remainingValues = values.get.run
      val nextValue =
        if (remainingValues.isEmpty)
          ZIO.die(
            new Exception(
              "Did not provide enough values!"
            )
          ).run
        else
          ZIO.succeed(remainingValues.head).run
      values.set(remainingValues.tail).run
      remainingValues.head
    }
end RandomBoundedIntFake

object RandomBoundedIntFake:
  def apply(
      values: Seq[Int]
  ): ZLayer[Any, Nothing, RandomBoundedInt] =
    ZLayer.fromZIO(
      defer {
        val valuesR = Ref.make(values).run
        new RandomBoundedIntFake(valuesR)
      }
    )
