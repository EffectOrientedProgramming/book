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
      resetPolicy = Retry.Schedules.common(factor = 1, jitterFactor = 0),
      onStateChange =
        state =>
          ZIO.debug(s"State change: $state")
    )

  val repeatSchedule =
    Schedule.recurs(7) &&
    Schedule.spaced(1.second)

  def run =
    defer:
      val cb = makeCircuitBreaker.run
      val numCalls = Ref.make[Int](0).run
      val protectedCall =
        cb(externalSystem(numCalls))
          .catchSome:
            case CircuitBreakerOpen =>
              ZIO.debug:
                "Circuit breaker blocked call"

      //externalSystem(numCalls)
      protectedCall
        .ignore
        .repeat(repeatSchedule)
        .run

      numCalls
        .get
        .debug("Calls to external system")
        .run

end CircuitBreakerDemo
