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
