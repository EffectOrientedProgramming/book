// EitherToZio.scala
package the_zio_type

import zio._

import java.io
import java.io.IOException

case class InvalidIntegerInput(value: String)

def parseInteger(
    input: String
): Either[InvalidIntegerInput, Int] =
  try
    Right(
      input.toInt
    ) // Right case is an integer
  catch
    case e: NumberFormatException =>
      Left(
        InvalidIntegerInput(input)
      ) // Left case is an error type
object EitherToZio extends ZIOAppDefault:

  val zEither: IO[InvalidIntegerInput, Int] =
    ZIO.fromEither(parseInteger("42"))

  def run = zEither
