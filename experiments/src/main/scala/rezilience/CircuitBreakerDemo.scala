package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*
import zio.{Schedule}

//case class Cost(value: Int)
//case class Analysis(content: String)

object CircuitBreakerDemo extends ZIOAppDefault:

  val makeCircuitBreaker =
    CircuitBreaker.make(
      trippingStrategy =
        TrippingStrategy
          .failureCount(maxFailures = 2),
      resetPolicy = Retry.Schedules.common(),
      onStateChange =
        state =>
          ZIO.debug(s"State change: $state")
    )

  def run =
    defer:
      val cb = makeCircuitBreaker.run
      cb(externalSystem)
        .tap(r => ZIO.debug(s"Result: $r"))
        .mapError:
          case CircuitBreakerOpen =>
            "Circuit breaker blocked the call to our external system"
          case WrappedError(e) =>
            s"External system threw an exception: $e"
        .tapError(e => ZIO.debug(e))
        .ignore
        .repeat(
          Schedule.recurs(30) &&
            Schedule.spaced(250.millis)
        )
        .run
//      expensiveSystem.billToDate.debug.run
//    .provide:
//       ExternalSystem // TOGGLE
//      ExternalSystemProtected // TOGGLE
//        .live

end CircuitBreakerDemo
