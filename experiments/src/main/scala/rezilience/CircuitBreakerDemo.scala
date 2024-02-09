package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*
import zio.Schedule


object CircuitBreakerDemo extends ZIOAppDefault:

  val makeCircuitBreaker =
    CircuitBreaker.make(
      trippingStrategy =
        TrippingStrategy
          .failureCount(maxFailures = 2),
      resetPolicy = Retry.Schedules.common(jitterFactor = 0),
      onStateChange =
        state =>
          ZIO.debug(s"State change: $state")
    )

  val repeatSchedule = Schedule.recurs(30) &&
    Schedule.spaced(250.millis)

  def run =
    defer:
      val cb = makeCircuitBreaker.run
      val numCallsRef = Ref.make[Int](0).run
      cb(externalSystem(numCallsRef))
        .tap(_ => ZIO.debug(s"Call to external service successful"))
        .mapError:
          case CircuitBreakerOpen =>
            "Circuit breaker blocked the call to our external system"
          case WrappedError(_) =>
            "External system threw an exception"
        .tapError(e => ZIO.debug(e))
        .ignore
        .repeat(repeatSchedule)
        .run

      numCallsRef.get.debug("We hit the external system this many times").run

end CircuitBreakerDemo
