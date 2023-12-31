package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*
import zio.{Schedule, ZLayer}

case class Cost(value: Int)

trait ExpensiveSystem:
  def call: ZIO[Any, String, Int]
  val billToDate:ZIO[Any, String, Cost]

object Scenario:
  enum Step:
    case Success,
      Failure

import rezilience.Scenario.Step
import Scenario.Step.*

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
          .exponentialBackoff(
            min = 1.second,
            max = 1.minute
          )
    )

  def run =
    defer:
      ZIO.serviceWithZIO[ExpensiveSystem](_.call)
        .ignore
        .repeat(Schedule.recurs(4))
        .run
      ZIO.serviceWithZIO[ExpensiveSystem](_.billToDate)
        .debug
        .run

    .provide:
       ExternalSystem // TOGGLE
//      ExternalSystemProtected // TOGGLE
        .withResponses:
          List(
            Success, Failure, Failure, Success
          )

end CircuitBreakerDemo

case class ExternalSystemProtected(
    externalSystem: ExpensiveSystem,
    circuitBreaker: CircuitBreaker[String]
                                  ) extends ExpensiveSystem:
  val billToDate: ZIO[Any, String, Cost] =
    externalSystem.billToDate

  def call: ZIO[Any, String, Int] =
    circuitBreaker:
      externalSystem.call
    .mapError:
      case CircuitBreakerOpen =>
        "Circuit breaker blocked the call to our external system"
      case WrappedError(e) =>
        s"External system threw an exception: $e"
    .tapError(e => ZIO.debug(e))

object ExternalSystemProtected:
  def withResponses(steps: List[Step]): ZLayer[Any, Nothing, ExpensiveSystem] =
    ZLayer.fromZIO:
      defer:
        ExternalSystemProtected(
          ZIO.service[ExpensiveSystem].run,
          CircuitBreakerDemo.makeCircuitBreaker.run
        )
      .provide(ExternalSystem.withResponses(steps), Scope.default)

// Invisible mdoc fencess

object ExternalSystem:
  def withResponses(
                     steps: List[Step]
                   ): ZLayer[Any, Nothing, ExpensiveSystem] =
    ZLayer.fromZIO:
      defer:
        val requests = Ref.make[Int](0).run
        ExternalSystem(
          requests,
          steps
        )

case class ExternalSystem(
                           requests: Ref[Int],
                           steps: List[Step]
                         ) extends ExpensiveSystem:

  // TODO: Better error type than Throwable
  val billToDate:ZIO[Any, String, Cost] =
    requests.get.map:
      Cost(_)

  def call: ZIO[Any, String, Int] =
    defer:
      ZIO.debug("Called underlying ExternalSystem").run
      val requestCount =
        requests.updateAndGet(_ + 1).run

      if (requestCount >= steps.length)
        ZIO
          .fail:
            "Something went wrong X"
          .run
      else
        steps.apply(requestCount) match
          case Scenario.Step.Success =>
            ZIO
              .succeed:
                requestCount
              .run
          case Scenario.Step.Failure =>
            ZIO
              .fail:
                "Something went wrong"
              .run
    .delay(500.millis)
end ExternalSystem
