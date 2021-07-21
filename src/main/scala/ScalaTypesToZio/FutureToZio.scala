//EitherToZio.scala
package ScalaTypesToZio

import zio._

import java.io
import java.io.IOException
import scala.concurrent.Future

class FutureToZio:

  lazy val sFuture =
    Future.successful("Success!")
