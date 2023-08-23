package cancellation

import scala.concurrent.Future

// We show that Future's are killed with finalizers that never run
object FutureNaiveCancellation
    extends ZIOAppDefault:

  def run =
    ZIO
      .fromFuture:
        Future:
          try Thread.sleep(500)
          finally println("Cleanup")
          "Success!"
      .timeout(25.millis)
      .debug
