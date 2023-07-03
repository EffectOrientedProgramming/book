package rezilience

import nl.vroste.rezilience.CircuitBreaker._
import nl.vroste.rezilience._
import zio._
import zio.direct._

object CircuitBreakerDemo extends ZIOAppDefault:
  case class ExternalSystem(
      requests: Ref[Int]
  ):
    object Scenario:
      enum Step:
        case Success, Failure

      import Step.*
      val steps = List(
        Success,
        Failure,
        Failure,
        Success,
      )

    // TODO: Better error type than Throwable
    def call(
          request: Int
      ): ZIO[Any, Throwable, Int] =
        defer {
          val requestCount =
            requests
              .getAndUpdate(_+1)
              .run

          Scenario.steps.apply(requestCount) match
            case Scenario.Step.Success =>
              ZIO.succeed(request)
                  .tap(result =>
                      ZIO.debug(
                        s"External system returned $result"
                      )
                    )
                  .run
            case Scenario.Step.Failure =>
              ZIO.fail(
                new Exception(
                  "Something went wrong"
                )
              ).run

        }.tapError(e =>
          ZIO.debug(s"External failed: $e")
        )
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

  def run =
    defer {
      val cb = makeCircuitBreaker.run
      val requests =
        Ref.make[Int](0).run
      val system = ExternalSystem(requests)
      ZIO
        .foreach(1 to 6)(id =>
          defer {
            ZIO.sleep(500.millis).run
            cb(system.call(id))
              .catchSome {
                case CircuitBreakerOpen =>
                  ZIO.debug(
                    "Circuit breaker blocked the call to our external system"
                  )
                case WrappedError(e) =>
                  ZIO.debug(
                    s"External system threw an exception: $e"
                  )
              }
              .run
          }
        )
        .run
    }
end CircuitBreakerDemo
