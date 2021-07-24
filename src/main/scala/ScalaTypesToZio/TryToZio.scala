// TryToZio.scala
package ScalaTypesToZio

import zio._
import java.io
import java.io.IOException
import scala.util.Try

class TryToZio:
  val dividend = 42
  val divisor = 7

  def sTry: Try[Int] = Try(dividend / divisor)

  val zTry: IO[Throwable, Int] =
    ZIO.fromTry(sTry)
