package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*
import zio.{Random, Schedule, ZIOAppDefault}

object CircuitBreakerSimple extends ZIOAppDefault {
  val makeCircuitBreaker =
    CircuitBreaker.make(
      trippingStrategy =
        TrippingStrategy
          .failureCount(maxFailures = 2),
      resetPolicy =
        Retry
          .Schedules
                    .common(),
//          .exponentialBackoff(
//            min = 1.second,
//            max = 4.second,
//          ),

      onStateChange =
        state =>
          ZIO.debug(s"State change: $state")
    )

  val spotty =
    defer:
      val shouldSucceed = Random.nextBoolean.run
      shouldSucceed match
        case true => ZIO.succeed(()).tap(_ => ZIO.debug("Succeeded")).run
        case false => ZIO.fail("Boom").tapError(_ => ZIO.debug("Failed")).run


  def run=
    defer:
      val cb = makeCircuitBreaker.run
      cb(spotty)
        .ignore
        .repeat(Schedule.recurs(30))
        .run

}
