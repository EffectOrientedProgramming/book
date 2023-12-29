package rezilience

import nl.vroste.rezilience.Bulkhead

/** In this demo, we can visualize all the
  * requests that are currently in flight
  */

// TODO - Should we show maxQueueing behavior?
val makeBulkhead: ZIO[Scope, Nothing, Bulkhead] =
  Bulkhead
    .make(maxInFlightCalls = 3)

object BulkheadDemo extends ZIOAppDefault:
  def run =
    defer:
      val bulkhead = makeBulkhead.run
      ZIO
        .foreachPar(1 to 10): _ =>
//          bulkhead:
            ZIO.serviceWithZIO[StatefulResource](_.request)
        .catchAll(error => ZIO.succeed(error))
        .debug("All requests done: ")
        .run
    .provideSome[Scope]:
      StatefulResource.live

// This will be invisible in the mdoc version.
// It can represent any service outside of our control
// that has usage constraints
case class StatefulResource(
    currentRequests: Ref[List[Int]],
    alive: Ref[Boolean]
):
  def request: ZIO[Any, String, Int] =
    defer:
      val res = Random.nextIntBounded(1000).run

      if (currentRequests.get.run.length > 3)
        alive.set(false).run
        println("Should die")
        ZIO.fail("Killed the server!!").run

      // Add request to current requests
      val numberOfCurrentRequests =
        currentRequests
          .updateAndGet(res :: _)
          .debug("Current requests: ")
          .run

      // Simulate a long-running request
      ZIO.sleep(1.second).run
      removeRequest(res).run

      if (alive.get.run)
        res
      else
        ZIO.fail("Server was killed by another request!!").run


  private def removeRequest(i: Int) =
    currentRequests.update(_ diff List(i))

end StatefulResource

object StatefulResource:

  val live =
    ZLayer.fromZIO:
      defer:
        StatefulResource(
          Ref.make[List[Int]](List.empty).run,
          Ref.make(true).run
        )
