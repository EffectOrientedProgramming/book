//FunctionToZio.scala
package ScalaTypesToZio

import zio._

import java.io
import java.io.IOException

class FunctionToZio:

  def sFunction(i: Int): Int =
    i * i

  val zFunction: URIO[
    Int,
    Int
  ] =
    ZIO.fromFunction(sFunction)
