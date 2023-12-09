package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.Bulkhead.BulkheadError

/** In this demo, we can visualize all the
  * requests that are currently in flight
  */

// TODO - Demonstrate when maxQueueing is reached
val makeBulkhead: ZIO[Scope, Nothing, Bulkhead] =
  Bulkhead
    .make(maxInFlightCalls = 3, maxQueueing = 32)

object BulkheadDemo extends ZIOAppDefault:
  def run =
    defer:
      val currentRequests =
        Ref.make[List[Int]](List.empty).run
      val bulkhead = makeBulkhead.run
      val statefulResource =
        StatefulResource(currentRequests)
      ZIO
        .foreachPar(1 to 10): _ =>
          bulkhead(statefulResource.request)
        .debug("All requests done: ")
        .run

case class StatefulResource(
    currentRequests: Ref[List[Int]]
):
  def request: ZIO[Any, Throwable, Int] =
    defer:
      val res = Random.nextIntBounded(1000).run
      // Add request to current requests
      currentRequests
        .updateAndGet(res :: _)
        .debug("Current requests: ")
        .run

      // Simulate a long-running request
      ZIO.sleep(1.second).run
      removeRequest(res).run

      res

  private def removeRequest(i: Int) =
    currentRequests.update(_ diff List(i))

end StatefulResource
