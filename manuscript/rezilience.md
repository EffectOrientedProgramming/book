## rezilience

 

### experiments/src/main/scala/rezilience/Bulkhead.scala
```scala
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

```


### experiments/src/main/scala/rezilience/CircuitBreakerDemo.scala
```scala
package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*

object Scenario:
  enum Step:
    case Success,
      Failure

import rezilience.Scenario.Step

object CircuitBreakerDemo extends ZIOAppDefault:
  case class ExternalSystem(
      requests: Ref[Int],
      steps: List[Step]
  ):

    // TODO: Better error type than Throwable
    def call(): ZIO[Any, Throwable, Int] =
      defer:
        val requestCount =
          requests.getAndUpdate(_ + 1).run

        steps.apply(requestCount) match
          case Scenario.Step.Success =>
            ZIO
              .succeed:
                requestCount
              .run
          case Scenario.Step.Failure =>
            ZIO
              .fail:
                Exception:
                  "Something went wrong"
              .run
      .tapError: e =>
        ZIO.debug(s"External failed: $e")
  end ExternalSystem

  val makeCircuitBreaker
      : ZIO[Scope, Nothing, CircuitBreaker[
        Any
      ]] =
    CircuitBreaker.make(
      trippingStrategy =
        TrippingStrategy
          .failureCount(maxFailures = 2),
      resetPolicy =
        Retry
          .Schedules
          .exponentialBackoff(
            min = 1.second,
            max = 1.minute
          )
    )

  def callProtectedSystem(
      cb: CircuitBreaker[Any],
      system: ExternalSystem
  ) =
    defer {
      ZIO.sleep(500.millis).run
      cb(system.call())
        .catchSome:
          case CircuitBreakerOpen =>
            ZIO.debug:
              "Circuit breaker blocked the call to our external system"
          case WrappedError(e) =>
            ZIO.debug:
              s"External system threw an exception: $e"
        .tap: result =>
          ZIO.debug:
            s"External system returned $result"
        .run
    }

  def run =
    defer:
      val cb       = makeCircuitBreaker.run
      val requests = Ref.make[Int](0).run
      import Scenario.Step.*

      val steps =
        List(Success, Failure, Failure, Success)
      val system =
        ExternalSystem(requests, steps)
      callProtectedSystem(cb, system)
        .repeatN(5)
        .run
end CircuitBreakerDemo

```


### experiments/src/main/scala/rezilience/RateLimiter.scala
```scala
package rezilience

import nl.vroste.rezilience.*

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
    defer:
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        // Repeats as fast as the limiter allows
        .repeatN(5).debug("Result").run

object RateLimiterDemoWithLogging
    extends ZIOAppDefault:

  // TODO Put in book-side ZIO helpers?
  extension [R, E, A](z: ZIO[R, E, A])
    def timedSecondsDebug(
        message: String
    ): ZIO[R, E, A] =
      z.timed
        .tap: (duration, res) =>
          ZIO.debug:
            message + ": " + res + " [took " +
              duration.getSeconds + "s]"
        .map(_._2)

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        // Print the time to generate each key:
        .timedSecondsDebug("Generated key")
        // Repeat as fast as the limiter allows:
        .repeatN(5)
        // Print the last result
        .timedSecondsDebug("Result").run

end RateLimiterDemoWithLogging

object RateLimiterDemoGlobal
    extends ZIOAppDefault:

  // TODO Put in book-side ZIO helpers?
  extension (z: ZIO.type)
    def repeatNPar[R, E, A](numTimes: Int)(
        op: Int => ZIO[R, E, A]
    ): ZIO[R, E, Seq[A]] =
      z.foreachPar(0 until numTimes)(op)

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      ZIO
        .repeatNPar(4): i =>
          rateLimiter(
            rsaKeyGenerator.debug(i.toString)
          )
            // Repeats as fast as allowed
            .repeatN(5).debug(s"Result $i")
        .run
end RateLimiterDemoGlobal

```


