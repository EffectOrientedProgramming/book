## rezilience

 

### experiments/src/main/scala/rezilience/CircuitBreakerDemo.scala
```scala
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

```


### experiments/src/main/scala/rezilience/CircuitBreakerInvisible.scala
```scala
package rezilience

import zio.Random

import java.time.Instant
import scala.concurrent.TimeoutException

// Invisible mdoc fencess

val externalSystem =
  defer:
    val b = Random.nextBoolean.run
    if b then
      ZIO.succeed("asdf").run
    else
      ZIO.fail("zxcv").run
//          scheduledValues(
//            (1.second, Success),
//            (3.seconds, Failure),
//            (5.seconds, Success)
//          ).run

/* case class ExternalSystem( requests: Ref[Int],
 * responseAction: ZIO[ Any, // access time
 * TimeoutException, Scenario.Step ] ) extends
 * ExpensiveSystem:
 *
 * // TODO: Better error type than Throwable val
 * billToDate: ZIO[Any, String, Cost] =
 * requests .get .map:
 * Cost(_)
 *
 * def call: ZIO[Any, String, Analysis] =
 * defer:
 * ZIO .debug( "Called underlying ExternalSystem"
 * ) .run val requestCount =
 * requests.updateAndGet(_ + 1).run
 * responseAction.orDie.run match case Success =>
 * ZIO .succeed:
 * Analysis:
 * s"Expensive report #$requestCount" .run case
 * Failure => ZIO .debug:
 * "boom" .run ZIO .fail:
 * "Something went wrong" .run
 *
 * end ExternalSystem */

// TODO Consider deleting
object InstantOps:
  extension (i: Instant)
    def plusZ(duration: zio.Duration): Instant =
      i.plus(duration.asJava)

import InstantOps._

/* Goal: If I accessed this from:
 * 0-1 seconds, I would get "First Value" 1-4
 * seconds, I would get "Second Value" 4-14
 * seconds, I would get "Third Value" 14+
 * seconds, it would fail */

// TODO Consider TimeSequence as a name
def scheduledValues[A](
    value: (Duration, A),
    values: (Duration, A)*
): ZIO[
  Any, // construction time
  Nothing,
  ZIO[
    Any, // access time
    TimeoutException,
    A
  ]
] =
  defer {
    val startTime = Clock.instant.run
    val timeTable =
      createTimeTableX(
        startTime,
        value,
        values* // Yay Scala3 :)
      )
    accessX(timeTable)
  }

// TODO Some comments, tests, examples, etc to
// make this function more obvious
private def createTimeTableX[A](
    startTime: Instant,
    value: (Duration, A),
    values: (Duration, A)*
): Seq[ExpiringValue[A]] =
  values.scanLeft(
    ExpiringValue(
      startTime.plusZ(value._1),
      value._2
    )
  ) {
    case (
          ExpiringValue(elapsed, _),
          (duration, value)
        ) =>
      ExpiringValue(
        elapsed.plusZ(duration),
        value
      )
  }

/** Input: (1 minute, "value1") (2 minute,
  * "value2")
  *
  * Runtime: Zero value: (8:00 + 1 minute,
  * "value1")
  *
  * case ((8:01, _) , (2.minutes, "value2")) =>
  * (8:01 + 2.minutes, "value2")
  *
  * Output: ( ("8:01", "value1"), ("8:03",
  * "value2") )
  */
private def accessX[A](
    timeTable: Seq[ExpiringValue[A]]
): ZIO[Any, TimeoutException, A] =
  defer {
    val now = Clock.instant.run
    ZIO
      .getOrFailWith(
        new TimeoutException("TOO LATE")
      ) {
        timeTable
          .find(_.expirationTime.isAfter(now))
          .map(_.value)
      }
      .run
  }

private case class ExpiringValue[A](
    expirationTime: Instant,
    value: A
)

```


### experiments/src/main/scala/rezilience/CircuitBreakerSimple.scala
```scala
package rezilience

import nl.vroste.rezilience.*
import nl.vroste.rezilience.CircuitBreaker.*
import zio.{Random, Schedule, ZIOAppDefault}

object CircuitBreakerSimple
    extends ZIOAppDefault:
  val makeCircuitBreaker =
    CircuitBreaker.make(
      trippingStrategy =
        TrippingStrategy
          .failureCount(maxFailures = 2),
      resetPolicy = Retry.Schedules.common(),
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
        case true =>
          ZIO
            .succeed(())
            .tap(_ => ZIO.debug("Succeeded"))
            .run
        case false =>
          ZIO
            .fail("Boom")
            .tapError(_ => ZIO.debug("Failed"))
            .run

  def run =
    defer:
      val cb = makeCircuitBreaker.run
      cb(spotty)
        .ignore
        .repeat(Schedule.recurs(30))
        .run
end CircuitBreakerSimple

```


### experiments/src/main/scala/rezilience/RateLimiter.scala
```scala
package rezilience

import nl.vroste.rezilience.RateLimiter

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
  Random.nextIntBounded(1000)

import zio_helpers.timedSecondsDebug
object RateLimiterDemoWithLogging
    extends ZIOAppDefault:

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        // Print the time to generate each key:
        .timedSecondsDebug("Generated key")
        // Repeat as fast as the limiter allows:
        .repeatN(3)
        // Print the last result
        .timedSecondsDebug("Result").run

object RateLimiterDemoGlobal
    extends ZIOAppDefault:

  import zio_helpers.repeatNPar

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      ZIO
        .repeatNPar(3): i =>
          rateLimiter:
            rsaKeyGenerator
          .timedSecondsDebug(
            s"${i.toString} generated a key"
          )
            // Repeats as fast as allowed
            .repeatN(2)
        .unit
        .timedSecondsDebug("Total time")
        .run
end RateLimiterDemoGlobal

```


