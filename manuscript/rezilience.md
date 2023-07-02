## rezilience

 

### experiments/src/main/scala/rezilience/Bulkhead.scala
```scala
package rezilience

import zio._
import zio.direct._
import nl.vroste.rezilience._
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
    defer {
      val currentRequests =
        Ref.make[List[Int]](List.empty).run
      val bulkhead = makeBulkhead.run
      val statefulResource =
        StatefulResource(currentRequests)
      ZIO
        .foreachPar(1 to 10)(_ =>
          bulkhead(statefulResource.request)
        )
        .debug("All requests done: ")
        .run
    }

case class StatefulResource(
    currentRequests: Ref[List[Int]]
):
  def request: ZIO[Any, Throwable, Int] =
    defer {
      val res = Random.nextInt.run
      // Add the request to the list of current
      // requests
      currentRequests
        .updateAndGet(res :: _)
        .debug("Current requests: ")
        .run

      // Simulate a long-running request
      ZIO.sleep(1.second).run
      // Remove the request from the list of
      // current requests
      currentRequests
        .update(
          _ diff List(res)
        ) // remove the request from the list
        .run

      res
    }
end StatefulResource

```


### experiments/src/main/scala/rezilience/RateLimiter.scala
```scala
package rezilience

import zio._
import zio.direct._
import nl.vroste.rezilience._

/** This is useful for scenarios such as:
  *   - Making sure you don't suddenly spike your
  *     AWS bill
  *   - Not accidentally DDOSing a service
  */
val makeRateLimiter
    : ZIO[Scope, Nothing, RateLimiter] =
  RateLimiter.make(max = 1, interval = 1.second)

// We use Throwable as error type in this example
def rsaKeyGenerator: ZIO[Any, Throwable, Int] =
  Random.nextInt

object RateLimiterDemo extends ZIOAppDefault:
  def run =
    defer {
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        .repeatN(
          5
        ) // This will repeat as fast as the limiter allows
        .debug("Result").run
    }

object RateLimiterDemoWithLogging
    extends ZIOAppDefault:
  def run =
    defer {
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        .timed // This shows the time it takes to generate each key
        .tap((duration, res) =>
          ZIO.debug(
            "Generated key: " + res + " in " +
              duration.getSeconds + " s"
          )
        )
        .map(_._2)
        .repeatN(5) // This will repeat as fast as the limiter allows
        .timed
        .map((duration, res) =>
          "Result: " + res + " in " +
            duration.getSeconds + " s"
        )
        .debug
        .run
    }
end RateLimiterDemoWithLogging

```


