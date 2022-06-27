// EitherToZio.scala
package the_zio_type

import zio.{ZIO, ZIOAppDefault}

import scala.util.{Left, Right}

case class InvalidIntegerInput(value: String)

object EitherToZio extends ZIOAppDefault:
  val goodInt: Either[InvalidIntegerInput, Int] =
    Right(42)

  val zEither
      : ZIO[Any, InvalidIntegerInput, Int] =
    ZIO.fromEither(goodInt)

  def run = zEither.debug("Converted Either")
