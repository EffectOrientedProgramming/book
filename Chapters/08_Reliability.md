# Reliability

[[Attempt by Bruce to create a chapter introduction]]

Reliability is a broad term with multiple meanings.
It is the ability of a system to perform and maintain routine functionality, in normal circumstances as well as high loads or hostile situations.

A basic meaning of reliability could be that your system builds and runs without any failures for all of its specified use cases.
If failures do occur, the system either recovers or shuts down in a well-defined manner.

Effects are the parts of your system that are unpredictable.
When we talk about reliability in terms of effects, the goal is to mitigate these unpredictabilities.
For example, if you make a request of a remote service, you don't know if the network is working or if that service is online.
Also, the service might be under a heavy load and will take a while to respond.
What we want is to be able to make a request and get a result in a reasonable amount of time.
If this is a problem, there are reliability strategies that generally involve inserting an intermediary that compensates for those issues.
For example, it might try one service, and if it doesn't get a response soon enough it makes other requests to other services.

In traditional coding, inserting these intermediaries can be a difficult and time-consuming process, often involving re-architecting to adapt to the new strategy.
If that strategy doesn't work, further rewriting may be required to try different strategies.
In a functional effect-based system, the goal is to be able to easily incorporate reliability strategies, and to easily change them if an approach doesn't work.
In this chapter we show ZIO components that can be attached to effects in order to improve their reliability.

## Caching
Putting a cache in front of a service can resolve many issues.

- If the service is slow, the cache can speed up the response time.
- If the service is brittle, the cache can provide a stable response and minimize the risk of overwhelming the resource.
- If the service is expensive, the cache can reduce the number of calls to it, and thus reduce your operating cost.

Putting a cache in front of a slow, brittle, or expensive service can be a great way to improve performance and reliability.

```scala mdoc:invisible
import zio.{ZIO, ZLayer}
import zio.cache.{Cache, Lookup}

import java.nio.file.{Path, Paths}

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
      .collectAllPar:
        List.fill(100):
          popularService.retrieve:
            Paths.get("awesomeMemes")
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

val popularService =
  ZLayer.fromZIO(makePopularService)
```

In this world, each request to our `CloudStorage` provider will cost us one dollar.
Egregious, but it will help us demonstrate the problem with small, round numbers.

```scala mdoc:runzio
def run =
  thunderingHerdsScenario
    .provide(CloudStorage.live, popularService)
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

```scala mdoc:runzio
def run =
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makeCachedPopularService)
  )
```

We can see that the invoice is only 1 dollar, because only one request reached our `CloudStorage` provider.
Wonderful!
In practice, the savings will rarely be *this* extreme, but it is a reassuring to know that we can handle these situations with ease, maintaining a low cost.

## Staying under rate limits

Rate limits are a common way to structure agreements between services.
In the worst case, going above this limit could overwhelm the service and make it crash.
At the very least, you will be charged more for exceeding it.

TODO Show un-limited demo first?

```scala mdoc:invisible
val expensiveApiCall =
  ZIO.unit

extension [R, E, A](z: ZIO[R, E, A])
  def timedSecondsDebug(
      message: String
  ): ZIO[R, E, A] =
    z.timed
      .tap:
        (duration, _) =>
          Console
            .printLine(
              message + " [took " +
                duration.getSeconds + "s]"
            )
            .orDie
      .map(_._2)
```

### Constructing a RateLimiter
Defining your rate limiter requires only the 2 pieces of information that should be codified in your service agreement:

```
$maxRequests / $interval
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
// TODO explain timedSecondsDebug
def makeCalls(name: String) =
  expensiveApiCall
    .timedSecondsDebug:
      s"$name called API"
    .repeatN(2) // Repeats as fast as allowed
```

Now, we wrap our unrestricted logic with our `RateLimiter`.
Even though the original code loops as fast the CPU allows, it will now adhere to our limit.

```scala mdoc:runzio:liveclock
def run =
  defer:
    val rateLimiter =
      makeRateLimiter.run
    rateLimiter:
      makeCalls:
        "System"
    .timedSecondsDebug("Result")
      .run
```

Most impressively, we can use the same `RateLimiter` across our application.
No matter the different users/features trying to hit the same resource, they will all be limited such that the entire application respects the rate limit.

```scala mdoc:runzio:liveclock
def run =
  defer:
    val rateLimiter =
      makeRateLimiter.run
    val people =
      List("Bill", "Bruce", "James")

    ZIO
      .foreachPar(people):
        person =>
          rateLimiter:
            makeCalls(person)
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
        ZIO.fail("Crashed the server!!").run

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
          .fail("Server crashed from requests!!")
          .run

  private def removeRequest(i: Int) =
    currentRequests.update(_ diff List(i))
end Live

object DelicateResource:
  val live =
    ZLayer.fromZIO:
      defer:
        Console
          .printLine:
            "Delicate Resource constructed."
          .run
        Console
          .printLine:
            "Do not make more than 3 concurrent requests!"
          .run
        Live(
          Ref.make[List[Int]](List.empty).run,
          Ref.make(true).run
        )
```

First, we demonstrate the unrestricted behavior:

```scala mdoc:runzio:liveclock
def run =
  defer:
    val delicateResource =
      ZIO.service[DelicateResource].run
    ZIO
      .foreachPar(1 to 10):
        _ => delicateResource.request
      .as("All Requests Succeeded!")
      .run
  .provideSome[Scope]:
    DelicateResource.live
```

We execute too many concurrent requests, and crash the server.
To prevent this, we need a `Bulkhead`.

```scala mdoc
import nl.vroste.rezilience.Bulkhead
val makeOurBulkhead =
  Bulkhead.make(maxInFlightCalls =
    3
  )
```

Next, we wrap our original request with this `Bulkhead`.

```scala mdoc:runzio:liveclock
def run =
  defer:
    val bulkhead =
      makeOurBulkhead.run

    val delicateResource =
      ZIO.service[DelicateResource].run
    ZIO
      .foreachPar(1 to 10):
        _ =>
          bulkhead:
            delicateResource.request
      .as("All Requests Succeeded")
      .run
  .provideSome[Scope]:
    DelicateResource.live
```

With this small adjustment, we now have a complex, concurrent guarantee.

## Circuit Breaking
Often, when a request fails, it is reasonable to immediately retry.
However, if we aggressively retry in an unrestricted way, we might actually make the problem worse by increasing the load on the struggling service.
Ideally, we would allow some number of aggressive retries, but then start blocking additional requests until the service has a chance to recover.

```scala mdoc:invisible
import zio.Ref

import java.time.Instant
import scala.concurrent.TimeoutException

// Invisible mdoc fencess
import zio.Runtime.default.unsafe
val timeSensitiveValue =
  Unsafe.unsafe(
    (u: Unsafe) =>
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

In this scenario, we are going to repeat our call many times in quick succession. 

```scala mdoc:silent
val repeatSchedule =
  Schedule.recurs(140) &&
    Schedule.spaced(50.millis)
```

When unrestrained, the code will let all the requests through to the degraded service.

```scala mdoc:runzio:liveclock
def run =
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

Now we will build our `CircuitBreaker`

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

Once again, the only thing that we need to do is wrap our original effect with the `CircuitBreaker`.

```scala mdoc:runzio:liveclock
def run =
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
{{TODO Fix output after `OurClock` changes}}
Now we see that our code prevented the majority of the doomed calls to the external service.

## Hedging

This technique snips off the most extreme end of the latency tail.

Determine the average response time for the top 50% of your requests.
If you make a call that does not get a response within this average delay, make an additional, identical request.
However, you do not give up on the 1st request, since it might respond immediately after sending the 2nd.
Instead, you want to race them and use the first response you get

To be clear - this technique will not reduce the latency of the fastest requests at all.
It only alleviates the pain of the slowest responses.

You have `1/n` chance of getting the worst case response time.
This approach turns that into a `1/n^2` chance.
The cost of this is only ~3% more total requests made. *Citations needed*

Further, if this is not enough to completely eliminate your extreme tail, you can employ the exact same technique once more.
Then, you end up with `1/n^3` chance of getting that worst performance.


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

```scala mdoc:runzio:liveclock
def run =
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

## Restricting Time
Sometimes, it's not enough to simply track the time that a test takes.
If you have specific Service Level Agreements (SLAs) that you need to meet, you want your tests to help ensure that you are meeting them.
However, even if you don't have contracts bearing down on you, there are still good reasons to ensure that your tests complete in a timely manner.
Services like GitHub Actions will automatically cancel your build if it takes too long, but this only happens at a very coarse level.
It simply kills the job and won't actually help you find the specific test responsible.

A common technique is to define a base test class for your project that all of your tests extend.
In this class, you can set a default upper limit on test duration.
When a test violates this limit, it will fail with a helpful error message.

This helps you to identify tests that have completely locked up, or are taking an unreasonable amount of time to complete.

For example, if you are running your tests in a CI/CD pipeline, you want to ensure that your tests complete quickly, so that you can get feedback as soon as possible.
you can use `TestAspect.timeout` to ensure that your tests complete within a certain time frame.

## Flakiness
Commonly, as a project grows, the supporting tests become more and more flaky.
This can be caused by a number of factors:

- The code is using shared, live services
  Shared resources, such as a database or a file system, might be altered by other processes.
  These could be other tests in the project, or even unrelated processes running on the same machine.

- The code is not thread safe
  Other processes running simultaneously might alter the expected state of the system.

- Resource limitations
  A team of engineers might be able to successfully run the entire test suite on their personal machines.
  However, the CI/CD system might not have enough resources to run the tests triggered by everyone pushing to the repository.
  Your tests might be occasionally failing due to timeouts or lack of memory.
