package the_zio_type

import zio.{ZIO, ZIOAppDefault}
import scala.concurrent.Future

object FutureToZio extends ZIOAppDefault:

  val zFuture =
    ZIO.fromFuture(implicit ec =>
      Future.successful("Success!")
    )

  val zFutureFailed =
    ZIO.fromFuture(implicit ec =>
      Future.failed(new Exception("Failure :("))
    )

  val run =
    zFutureFailed.debug("Converted Future")
