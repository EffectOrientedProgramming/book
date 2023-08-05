package cancellation

import scala.concurrent.{
  ExecutionContext,
  Future
}
import scala.concurrent.ExecutionContext.Implicits.global

// We show that Future's are killed with finalizers that never run
object FutureNaiveCancellation
    extends ZIOAppDefault:

  def run =
    ZIO
      .fromFuture:
        Future:
          tryFinally
          "Success!"
      .timeout(25.millis)
      .debug

  def tryFinally =
    try Thread.sleep(500)
    finally println("Cleanup")

object FutureCancellation extends ZIOAppDefault:
  def run =
    ZIO
      .attempt:
        Future:
          Thread.sleep(1000)
          "Success!"
      .debug
