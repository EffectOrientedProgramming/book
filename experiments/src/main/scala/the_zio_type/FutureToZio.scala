package the_zio_type

import zio._

import java.io
import java.io.IOException
import scala.concurrent.Future

object FutureToZio extends ZIOAppDefault:

  lazy val sFuture: Future[String] =
    Future.successful("Success!")
    // Future.failed(new Exception("Failure :("))

  val run =
    ZIO.fromFuture( implicit ec => sFuture)
