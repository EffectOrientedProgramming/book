# Reliability

```scala 3
// TODO: really "advanced recover techniques" as basic ones should have already been covered
//    Bill - Disagree with this name/framing proposal. "Recovery" is for when things have already gone wrong, and you're trying to respond to them.
//           Caching, RateLimiting, Bulk-heading (and possibly Hedging) are all _proactive_ steps we take to ensure the program runs as
//           desired. There might be a better name here, but I think "recovery" is a step in the wrong direction.
```

For our purposes,
  A reliable system behaves predictably in normal circumstances as well as high loads or even hostile situations.
If failures do occur, the system either recovers or shuts down in a well-defined manner.

Effects are the parts of your system that are unpredictable.
When we talk about reliability in terms of Effects, the goal is to mitigate these unpredictabilities.
For example, if you make a request of a remote service, you don't know if the network is working or if that service is online.
Also, the service might be under a heavy load and slow to respond.
There are strategies to compensate for those issues without invasive restructuring.
For example, we can attach fallback behavior:
  make a request to our preferred service, and if we don't get a response soon enough, make a request to a secondary service.

Traditional coding often requires extensive re-architecting to apply and adapt reliability strategies, and further rewriting if they fail.
In a functional Effect-based system, reliability strategies can be easily incorporated and modified.
This chapter demonstrates components that enhance Effect reliability.

## Caching

Putting a cache in front of a service can resolve when a service is:

- Slow: the cache can speed up the response time.
- Brittle: the cache can provide a stable response and minimize the risk of overwhelming the resource.
- Expensive: the cache can reduce the number of calls to it, and thus reduce your operating cost.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

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

case class FileContents(
    contents: List[String]
)

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
If you have a steady stream of requests coming in, any naive cache can store the result after the first request, and then be ready to serve it to all subsequent requests.
However, it is possible that all the requests will arrive before the first one has been served and the value has been cached.
In this case, a naive cache would cause each request to call the underlying slow/brittle/expensive service and then they would each update the cache with the identical value.

Here is what that looks like:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import zio.*

val thunderingHerds =
  defer:
    val popularService =
      ZIO.service[PopularService].run

    // All requests arrive at once
    ZIO
      .collectAllPar:
        List.fill(100):
          popularService.retrieve:
            Paths.get("awesomeMemes")
      .run

    val cloudStorage =
      ZIO.service[CloudStorage].run

    cloudStorage.invoice.run
```

We first show the uncached service:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val makePopularService =
  defer:
    val cloudStorage =
      ZIO.service[CloudStorage].run
    PopularService(cloudStorage.retrieve)
```

To construct a `PopularService`, we give it the Effect that looks up content.
In this version, it goes directly to the `CloudStorage` provider.

Suppose each request to our `CloudStorage` provider costs one dollar.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  thunderingHerds.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makePopularService)
  )
```

The invoice is 100 dollars because every single request reached our `CloudStorage` provider.

Now let's construct a `PopularService` that uses a cache:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

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

- Building the cache with sensible values
- Passing the `Cache.get` method to the `PopularService` constructor, instead of the bare `CloudStorage.retrieve` method

Now we run the same scenario with the cache in place:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  thunderingHerds.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makeCachedPopularService)
  )
```

The invoice is only 1 dollar, because only one request reached the `CloudStorage` provider.
Wonderful!
In practice, the savings will rarely be *this* extreme, but it is reassuring to know we can handle these situations with ease.

## Staying under rate limits

Rate limits are a common way to structure agreements between services.
In the worst case, going above this limit could overwhelm the service and make it crash.
At the very least, you will be charged more for exceeding it.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*
import zio.Console._

val expensiveApiCall =
  ZIO.unit

extension [R, E, A](z: ZIO[R, E, A])
  def timedSecondsDebug(
      message: String
  ): ZIO[R, E, A] =
    z.timed
      .tap:
        (duration, _) =>
          printLine(
            message + " [took " +
              duration.getSeconds + "s]"
          ).orDie
      .map(_._2)
```

Defining your rate limiter requires only the 2 pieces of information that should be codified in your service agreement:

```psuedo
$maxRequests / $interval
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import nl.vroste.rezilience.RateLimiter

val makeRateLimiter =
  RateLimiter.make(
    max =
      1,
    interval =
      1.second
  )
```

Now, we wrap our unrestricted logic with our `RateLimiter`.
Even though the original code loops as fast the CPU allows, it will now adhere to our limit.

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

def run =
  defer:
    val rateLimiter =
      makeRateLimiter.run

    rateLimiter:
      expensiveApiCall
    .timedSecondsDebug:
      s"called API"
      // Repeats as fast as allowed
    .repeatN:
      2
    .timedSecondsDebug:
      "Result"
    .run
```

Most impressively, we can use the same `RateLimiter` across our application.
No matter the different users/features trying to hit the same resource, they will all be limited such that the entire application respects the rate limit.

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

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
            expensiveApiCall
          .timedSecondsDebug:
            s"$person called API"
          .repeatN(
            2
          ) // Repeats as fast as allowed
      .timedSecondsDebug:
        "Total time"
      .unit // ignores the list of unit
      .run
```

## Constraining concurrent requests

If we want to ensure we don't accidentally DDOS a service, we can restrict the number of concurrent requests to it.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*
import zio.Console._

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
        .debug("Current requests")
        .run

      // Simulate a long-running request
      ZIO.sleep(1.second).run
      removeRequest(res).run

      if (alive.get.run)
        res
      else
        ZIO
          .fail(
            "Server crashed from requests!!"
          )
          .run

  private def removeRequest(i: Int) =
    currentRequests.update(_ diff List(i))
end Live

object DelicateResource:
  val live =
    ZLayer.fromZIO:
      defer:
        printLine:
          "Delicate Resource constructed."
        .run
        printLine:
          "Do not make more than 3 concurrent requests!"
        .run
        Live(
          Ref
            .make[List[Int]](List.empty)
            .run,
          Ref.make(true).run
        )
```

First, we demonstrate the unrestricted behavior:

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

def run =
  defer:
    val delicateResource =
      ZIO.service[DelicateResource].run
    ZIO
      .foreachPar(1 to 10):
        _ => delicateResource.request
      .as("All Requests Succeeded!")
      .run
  .provide(DelicateResource.live)
```

We execute too many concurrent requests, and crash the server.
To prevent this, we need a `Bulkhead`.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import nl.vroste.rezilience.Bulkhead
val makeOurBulkhead =
  Bulkhead.make(maxInFlightCalls =
    3
  )
```

Next, we wrap our original request with this `Bulkhead`.

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

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
  .provide(
    DelicateResource.live,
    Scope.default
  )
```

With this small adjustment, we now have a complex, concurrent guarantee.

## Circuit Breaking

Often, when a request fails, it is reasonable to immediately retry.
However, if we aggressively retry in an unrestricted way, we might actually make the problem worse by increasing the load on the struggling service.
Ideally, we would allow some number of aggressive retries, but then start blocking additional requests until the service has a chance to recover.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

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
      ZIO.unit.run
    else
      ZIO.fail(()).run

object InstantOps:
  extension (i: Instant)
    def plusZ(
        duration: zio.Duration
    ): Instant =
      i.plus(duration.asJava)

import InstantOps._

/* Goal: If I accessed this from:
 * 0-1 seconds, I would get "First Value" 1-4
 * seconds, I would get "Second Value" 4-14
 * seconds, I would get "Third Value" 14+
 * seconds, it would fail */

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
  * case ((8:01, _) , (2.minutes, "value2"))
  * \=> (8:01 + 2.minutes, "value2")
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
          .find(
            _.expirationTime.isAfter(now)
          )
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

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val repeatSchedule =
  Schedule.recurs(140) &&
    Schedule.spaced(50.millis)
```

When unrestrained, the code will let all the requests through to the degraded service.

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

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

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import nl.vroste.rezilience.{
  CircuitBreaker,
  TrippingStrategy,
  Retry
}

val makeCircuitBreaker =
  CircuitBreaker.make(
    trippingStrategy =
      TrippingStrategy
        .failureCount(maxFailures =
          2
        ),
    resetPolicy =
      Retry.Schedules.common()
  )
```

Once again, the only thing that we need to do is wrap our original Effect with the `CircuitBreaker`.

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

import CircuitBreaker.CircuitBreakerOpen

def run =
  defer:
    val cb =
      makeCircuitBreaker.run

    val numCalls =
      Ref.make[Int](0).run
    val numPrevented =
      Ref.make[Int](0).run

    val protectedCall =
      // TODO Note/explain `catchSome`
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
    s"Prevented: $prevented Made: $made"
```

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

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

val logicThatSporadicallyLocksUp =
  defer:
    if (
      Random.nextIntBounded(1_000).run == 0
    )
      ZIO
        .sleep:
          3.second
        .run
```

```scala 3 mdoc:invisible
case class LogicHolder(
    logic: ZIO[Any, Nothing, Unit]
)
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

// TODO LogicHolder is the only I could think
//  of to let us pass in the original and
//  hedged logic without re-writing everything
//  around it. Worthwhile, or just a
//  complicated distraction?
def businessLogic(logicHolder: LogicHolder) =
  defer:
    val makeRequest =
      logicHolder
        .logic
        .timeoutFail("took too long")(
          1.second
        )

    val totalRequests =
      50_000

    val successes =
      ZIO
        .collectAllSuccessesPar(
          List
            .fill(totalRequests)(makeRequest)
        )
        .run

    val contractBreaches =
      totalRequests - successes.length

    "Contract Breaches: " + contractBreaches
```

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

def run =
  businessLogic:
    LogicHolder:
      logicThatSporadicallyLocksUp
```

Sadly, we have breached our contract many times in this scenario.
Now, what would it take to build our hedged logic?

```scala 3 mdoc:silent
val hedged =
  logicThatSporadicallyLocksUp.race:
    logicThatSporadicallyLocksUp.delay:
      25.millis
```

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

def run =
  businessLogic:
    LogicHolder:
      hedged
```

## Test Reliability

ZIO Test has a feature called `TestAspect`s.
These are used to attach additional capabilities or restrictions to your tests, while keeping your test code clean and focused.

ZIO includes several pre-made `TestAspect`s, but we will highlight just few that we have found most useful.
You are also free to make your own, attaching any capabilities/restrictions that are pertinent to your domain

### Test Timeouts

Sometimes, it's not enough to simply track the time that a test takes.
If you have specific Service Level Agreements (SLAs) that you need to meet, you want your tests to help ensure that you are meeting them.
However, even if you don't have contracts bearing down on you, there are still good reasons to ensure that your tests complete in a timely manner.
Services like GitHub Actions will automatically cancel your build if it takes too long, but this only happens at a very coarse level.
It simply kills the job and won't actually help you find the specific test responsible.

A common technique is to define a base test class for your project that all of your tests extend.
In this class, you can set a default upper limit on test duration.
When a test violates this limit, it will fail with a helpful message.

This helps you to identify tests that have completely locked up, or are taking an unreasonable amount of time to complete.

For example, if you are running your tests in a CI/CD pipeline, you want to ensure that your tests complete quickly, so that you can get feedback as soon as possible.
you can use `TestAspect.timeout` to ensure that your tests complete within a certain time frame.

```scala 3 mdoc:testzio manuscript-only
import zio.test.*

def spec =
  test("long testZ"):
    defer:
      ZIO.sleep(1.hour).run
      assertCompletes
  @@ TestAspect.withLiveClock @@
    TestAspect.timeout(1.second)
```

### Flaky Tests

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

```scala 3 mdoc:invisible
import zio.Console._

val attemptsR =
  Unsafe.unsafe {
    implicit unsafe =>
      Runtime
        .default
        .unsafe
        .run(Ref.make(0))
        .getOrThrowFiberFailure()
  }

def spottyLogic =
  defer:
    val attemptsCur =
      attemptsR.getAndUpdate(_ + 1).run
    if ZIO.attempt(attemptsCur).run == 3 then
      printLine("Success!").run
      ZIO.succeed(1).run
    else
      printLine("Failed!").run
      ZIO.fail("Failed").run
```

```scala 3 mdoc:testzio
import zio.test.*
import zio.Console._

def spec =
  test("flaky test!"):
    defer:
      spottyLogic.run
      printLine("Continuing...").run
      assertCompletes
  @@ TestAspect.flaky
```
