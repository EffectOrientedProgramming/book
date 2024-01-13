package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*
import zio.{Schedule, ZLayer}

case class Cost(value: Int)
case class Analysis(content: String)

trait ExpensiveSystem:
  def call: ZIO[Any, String, Analysis]
  val billToDate:ZIO[Any, String, Cost]

object Scenario:
  enum Step:
    case Success,
      Failure

object CircuitBreakerDemo extends ZIOAppDefault:

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
          .common(),
//          .exponentialBackoff(
//            min = 1.second,
//            max = 4.second,
//          ),

      onStateChange = (state) =>
        ZIO.debug(
          s"State change: $state"
        )
        
    )

  def run =
    defer:
      ZIO.serviceWithZIO[ExpensiveSystem](_.call)
        .ignore
        .repeat(Schedule.recurs(8) && Schedule.spaced(200.millis))
        .run
      ZIO.serviceWithZIO[ExpensiveSystem](_.billToDate)
        .debug
        .run

    .provide:
//       ExternalSystem // TOGGLE
      ExternalSystemProtected // TOGGLE
        .live

end CircuitBreakerDemo

case class ExternalSystemProtected(
    externalSystem: ExpensiveSystem,
    circuitBreaker: CircuitBreaker[String]
                                  ) extends ExpensiveSystem:
  val billToDate: ZIO[Any, String, Cost] =
    externalSystem.billToDate

  def call: ZIO[Any, String, Analysis] =
    circuitBreaker:
      externalSystem.call
    .tap(r => ZIO.debug(s"Result: $r"))
    .mapError:
      case CircuitBreakerOpen =>
        "Circuit breaker blocked the call to our external system"
      case WrappedError(e) =>
        println("ignored boom?")
        s"External system threw an exception: $e"
    .tapError(e => ZIO.debug(e))

object ExternalSystemProtected:
  val live: ZLayer[Any, Nothing, ExpensiveSystem] =
    ZLayer.fromZIO:
      defer:
        ExternalSystemProtected(
          ZIO.service[ExpensiveSystem].run,
          CircuitBreakerDemo.makeCircuitBreaker.run
        )
      .provide(ExternalSystem.live, Scope.default)
