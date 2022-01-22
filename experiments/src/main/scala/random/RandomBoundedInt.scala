package random

import zio.{Tag, UIO, ZEnv, ZIO, ZIOAppArgs}
import scala.util.Random

trait RandomBoundedInt:
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int]

import zio.{UIO, ZIO, ZLayer}

import scala.util.Random

object RandomBoundedInt:
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): ZIO[RandomBoundedInt, Nothing, Int] =
    ZIO.serviceWithZIO[RandomBoundedInt](
      _.nextIntBetween(
        minInclusive,
        maxExclusive
      )
    )

  val live
      : ZLayer[Any, Nothing, RandomBoundedInt] =
    ZLayer.succeed(
      new RandomBoundedInt:
        override def nextIntBetween(
            minInclusive: Int,
            maxExclusive: Int
        ): UIO[Int] =
          ZIO.succeed(
            Random.between(
              minInclusive,
              maxExclusive
            )
          )
    )
end RandomBoundedInt
