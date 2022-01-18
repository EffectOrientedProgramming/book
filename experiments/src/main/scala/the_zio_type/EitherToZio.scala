// EitherToZio.scala
package ScalaTypesToZio

import zio._

import java.io
import java.io.IOException

class EitherToZio:
  // Depending on the input, sEither can be a
  // String or an Int
  val input = "I am a string"

  val sEither: Either[String, Int] =
    try
      Right(
        input.toInt
      ) // Right case is an integer
    catch
      case e: NumberFormatException =>
        Left(input) // Left case is an error type

  // We can translate this directly into a more
  // succinct and readable IO.

  val zEither: IO[String, Int] =
    ZIO.fromEither(sEither)
end EitherToZio
