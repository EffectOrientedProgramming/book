# Reliability

Reliability is the ability of a system to perform and maintain its functions in routine circumstances, as well as hostile or unexpected circumstances. 
It is a measure of the quality of a system and is a key factor in the success of any system.
It can cover a wide range of topics, from the ability to handle errors, to the ability to handle high loads, to the ability to handle malicious attacks.
We will not cover all of these topics, but will highlight some important ones that ZIO handles nicely.

## Caching
Putting a cache in front of a service can resolve many issues.

- If the service is slow, the cache can speed up the response time.
- If the service is brittle, the cache can provide a stable response and minimize the risk of overwhelming the resource.
- If the service is expensive, the cache can reduce the number of calls to it, and thus reduce your operating cost.

Putting a cache in front of a slow, brittle, or expensive service can be a great way to improve performance and reliability.

```scala mdoc:invisible
import zio.{ZIO, ZLayer}
import zio.cache.{Cache, Lookup}

import java.nio.file.Path

case class FSLive(requests: Ref[Int])
    extends CloudStorage:
  def retrieve(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    defer:
      requests.update(_ + 1).run
      ZIO.sleep(10.millis).run
      FSLive.hardcodedContents

  val invoice: ZIO[Any, Nothing, String] =
    defer:
      val count =
        requests.get.run

      "Amount owed: $" + count

object FSLive:
  val hardcodedContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )

case class FileContents(contents: List[String])

trait CloudStorage:
  def retrieve(
      name: Path
  ): ZIO[Any, Nothing, FileContents]
  val invoice: ZIO[Any, Nothing, String]

object CloudStorage:
  val live =
    ZLayer.fromZIO:
      defer:
        FSLive(Ref.make(0).run)

case class PopularService(
    retrieveContents: Path => ZIO[
      Any,
      Nothing,
      FileContents
    ]
):
  def retrieve(name: Path) =
    retrieveContents(name)
```

To demonstrate, we will take one of the worst case scenarios that your service might encounter: the thundering herd problem.

```scala mdoc:silent
val thunderingHerdsScenario =
  defer:
    val popularService =
      ZIO.service[PopularService].run

    ZIO // All requests arrives nearly at once
      .foreachPar(List.fill(100)(())):
        _ => // james don't like
          popularService.retrieve:
            Path.of("awesomeMemes")
      .run

    val cloudStorage =
      ZIO.service[CloudStorage].run

    cloudStorage.invoice.debug.run
```

If you have a steady stream of requests coming in, any naive cache can store the result after the first request, and then be ready to serve it to all subsequent requests.
However, it is possible that all the requests will arrive before the first one has been served and cached the value.
In this case, a naive cache would allow all of them to trigger their own request to your underlying slow/brittle/expensive service and then they would all update the cache with the same value.
Thankfully, ZIO provides capabilities that make it easy to capture simultaneous requests to the same resource, and make sure that only one request is made to the underlying service.

We will first show the uncached service:

```scala mdoc:silent
val makePopularService =
  defer:
    val cloudStorage =
      ZIO.service[CloudStorage].run
    PopularService(cloudStorage.retrieve)
```

In this world, each request to our `CloudStorage` provider will cost us one dollar.
Egregious, but it will help us demonstrate the problem with small, round numbers.

```scala mdoc
runDemo:
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makePopularService)
  )
```

We can see that the invoice is 100 dollars, because every single request reached our `CloudStorage` provider.

Now we will apply our cache:

```scala mdoc:silent
val makeCachedPopularService =
  defer:
    val cloudStorage =
      ZIO.service[CloudStorage].run
    val cache =
      Cache
        .make(
          capacity =
            100,
          timeToLive =
            Duration.Infinity,
          lookup =
            Lookup(cloudStorage.retrieve)
        )
        .run

    PopularService(cache.get)
```

The only changes required are:

- building our cache with sensible values
- then passing the `Cache#get` method to our `PopularService` constructor, rather than the bare `CloudStorage#retrieve` method

Now when we run the same scenario, with our cache in place:

```scala mdoc
runDemo:
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makeCachedPopularService)
  )
```

We can see that the invoice is only 1 dollar, because only one request reached our `CloudStorage` provider.
Wonderful!
In practice, the savings will rarely be *this* extreme, but it is a reassuring to know that we can handle these situations with ease, while maintaining a low cost.

## Staying under rate limits

```scala mdoc:invisible
val expensiveApiCall =
  ZIO.unit

extension [R, E, A](z: ZIO[R, E, A])
  def timedSecondsDebug(
      message: String
  ): ZIO[R, E, A] =
    z.timed
      .tap: (duration, _) =>
        println(
          message + " [took " +
            duration.getSeconds + "s]"
        )
        ZIO.unit
      .map(_._2)
```

```scala mdoc:silent
import nl.vroste.rezilience.RateLimiter

val makeRateLimiter =
  RateLimiter.make(
    max =
      1,
    interval =
      1.second
  )
```

```scala mdoc:silent
// shows extension function definition
// so that we can explain timedSecondsDebug
extension (rateLimiter: RateLimiter)
  def makeCalls(name: String) =
    rateLimiter:
      expensiveApiCall
    .timedSecondsDebug:
      s"$name called API"
    .repeatN(2) // Repeats as fast as allowed
```

```scala mdoc
runDemo:
  defer:
    val rateLimiter =
      makeRateLimiter.run
    rateLimiter
      .makeCalls:
        "System"
      .timedSecondsDebug("Result")
      .run
```

```scala mdoc
runDemo:
  defer:
    val rateLimiter =
      makeRateLimiter.run
    val people =
      List("Bill", "Bruce", "James")

    ZIO
      .foreachPar(people):
        rateLimiter.makeCalls
      .timedSecondsDebug:
        "Total time"
      .run
```

## Constraining concurrent requests
If we want to ensure we don't accidentally DDOS a service, we can restrict the number of concurrent requests to it.


```scala mdoc:invisible
trait DelicateResource:
  val request: ZIO[Any, String, Int]
// It can represent any service outside of our control
// that has usage constraints
case class Live(
    currentRequests: Ref[List[Int]],
    alive: Ref[Boolean]
) extends DelicateResource:
  val request =
    defer:
      val res =
        Random.nextIntBounded(1000).run

      if (currentRequests.get.run.length > 3)
        alive.set(false).run
        ZIO.fail("Killed the server!!").run

      // Add request to current requests
      currentRequests
        .updateAndGet(res :: _)
        .debug("Current requests: ")
        .run

      // Simulate a long-running request
      ZIO.sleep(1.second).run
      removeRequest(res).run

      if (alive.get.run)
        res
      else
        ZIO
          .fail(
            "Server was killed by another request!!"
          )
          .run

  private def removeRequest(i: Int) =
    currentRequests.update(_ diff List(i))

end DelicateResource

object DelicateResource:
  val live =
    ZLayer.fromZIO:
      defer:
        ZIO
          .debug(
            "Delicate Resource constructed."
          )
          .run
        ZIO
          .debug(
            "Do not make more than 3 concurrent requests!"
          )
          .run
        Live(
          Ref.make[List[Int]](List.empty).run,
          Ref.make(true).run
        )
```

```scala mdoc
runDemo:
  defer:
    val delicateResource =
      ZIO.service[DelicateResource].run
    ZIO
      .foreachPar(1 to 10): _ =>
        //          bulkhead:
        delicateResource.request
      .as("All Requests Succeeded")
      .catchAll(err => ZIO.succeed(err))
      .debug
      .run
  .provideSome[Scope]:
    DelicateResource.live
```

```scala mdoc
import nl.vroste.rezilience.Bulkhead

runDemo:
  defer:
    val bulkhead: Bulkhead =
      Bulkhead
        .make(maxInFlightCalls =
          3
        )
        .run
    val delicateResource =
      ZIO.service[DelicateResource].run
    ZIO
      .foreachPar(1 to 10): _ =>
        bulkhead:
          delicateResource.request
      .as("All Requests Succeeded")
      .catchAll(err => ZIO.succeed(err))
      .debug
      .run
  .provideSome[Scope]:
    DelicateResource.live
```

## Circuit Breaking

```scala mdoc:invisible
import zio.Ref

import java.time.Instant
import scala.concurrent.TimeoutException

// Invisible mdoc fencess
import zio.Runtime.default.unsafe
val timeSensitiveValue =
  Unsafe.unsafe((u: Unsafe) =>
    given Unsafe =
      u
    unsafe
      .run(
        scheduledValues(
          (1100.millis, true),
          (4100.millis, false),
          (5000.millis, true)
        )
      )
      .getOrThrowFiberFailure()
  )

def externalSystem(numCalls: Ref[Int]) =
  defer:
    numCalls.update(_ + 1).run
    val b =
      timeSensitiveValue.run
    if b then
      ZIO.succeed(()).run
    else
      ZIO.fail(()).run

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
    val startTime =
      Clock.instant.run
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
    val now =
      Clock.instant.run
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

```scala mdoc:silent
val repeatSchedule =
  Schedule.recurs(140) &&
    Schedule.spaced(50.millis)
```

```scala mdoc
runDemo:
  defer:
    val numCalls =
      Ref.make[Int](0).run

    externalSystem(numCalls)
      .ignore
      .repeat(repeatSchedule)
      .run

    val made =
      numCalls.get.run

    s"Calls made: $made"
```

```scala mdoc:silent
import nl.vroste.rezilience.{
  CircuitBreaker,
  TrippingStrategy,
  Retry
}
import nl.vroste.rezilience.CircuitBreaker.CircuitBreakerOpen

val makeCircuitBreaker =
  CircuitBreaker.make(
    trippingStrategy =
      TrippingStrategy.failureCount(maxFailures =
        2
      ),
    resetPolicy =
      Retry.Schedules.common()
  )
```

```scala mdoc
runDemo:
  defer:
    val cb =
      makeCircuitBreaker.run
    val numCalls =
      Ref.make[Int](0).run
    val numPrevented =
      Ref.make[Int](0).run
    val protectedCall =
      cb(externalSystem(numCalls)).catchSome:
        case CircuitBreakerOpen =>
          numPrevented.update(_ + 1)

    protectedCall
      .ignore
      .repeat(repeatSchedule)
      .run

    val prevented =
      numPrevented.get.run

    val made =
      numCalls.get.run
    s"Calls prevented: $prevented Calls made: $made"
```

## Hedging


```scala mdoc:invisible
val logicThatSporadicallyLocksUp =
  defer:
    val random =
      Random.nextIntBounded(1_000).run
    random match
      case 0 =>
        ZIO
          .sleep:
            3.seconds
          .run
        ZIO
          .succeed:
            2.second
          .run
      case _ =>
        10.millis
```

```scala mdoc
runDemo:
  defer:
    val contractBreaches =
      Ref.make(0).run

    ZIO
      .foreachPar(List.fill(50_000)(())):
        _ => // james still hates this
          defer:
            val hedged =
              logicThatSporadicallyLocksUp.race:
                logicThatSporadicallyLocksUp
                  .delay:
                    25.millis

            // TODO How do we make this demo more
            // obvious?
            // The request is returning the
            // hypothetical runtime, but that's
            // not clear from the code that will
            // be visible to the reader.
            val duration =
              hedged.run
            if (duration > 1.second)
              contractBreaches.update(_ + 1).run
      .run

    contractBreaches
      .get
      .debug("Contract Breaches")
      .run
```

