package std_type_conversions_to_zio

import zio._
import java.io
import java.io.IOException
import scala.util.Try

object TryToZio extends ZIOAppDefault:
  val dividend = 42
  val divisor  = 7

  // Significant Note: Try is a standard
  // collection by-name function. This makes
  // it a good candidate for introducting that
  // concept.
  def sTry: Try[Int] = Try(dividend / divisor)

  val zTry: IO[Throwable, Int] =
    ZIO.fromTry(sTry)

  val run = zTry
