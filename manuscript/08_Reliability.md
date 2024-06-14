# Reliability


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/08_Reliability.md)


{{ really "advanced recover techniques" as basic ones should have already been covered }}

For our purposes,
  A reliable system behaves predictably in normal circumstances as well as high loads or even hostile situations.
If failures do occur, the system either recovers or shuts down in a well-defined manner.

Effects are the parts of your system that are unpredictable.
When we talk about reliability in terms of effects, the goal is to mitigate these unpredictabilities.
For example, if you make a request of a remote service, you don't know if the network is working or if that service is online.
Also, the service might be under a heavy load and slow to respond.
There are strategies to compensate for those issues without invasive restructuring.
For example, we can attach fallback behavior:
  make a request to our preferred service, and if we don't get a response soon enough, make a request to a secondary service.

Traditional coding often requires extensive re-architecting to apply and adapt reliability strategies, and further rewriting if they fail.
In a functional effect-based system, reliability strategies can be easily incorporated and modified.
This chapter demonstrates components that enhance effect reliability.

## Caching

Putting a cache in front of a service can resolve when a service is:

- Slow: the cache can speed up the response time.
- Brittle: the cache can provide a stable response and minimize the risk of overwhelming the resource.
- Expensive: the cache can reduce the number of calls to it, and thus reduce your operating cost.


To demonstrate, we will take one of the worst case scenarios that your service might encounter: the thundering herd problem.
If you have a steady stream of requests coming in, any naive cache can store the result after the first request, and then be ready to serve it to all subsequent requests.
However, it is possible that all the requests will arrive before the first one has been served and the value has been cached.
In this case, a naive cache would cause each request to call the underlying slow/brittle/expensive service and then they would each update the cache with the identical value.

Here is what that looks like:

```scala
val thunderingHerdsScenario =
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

```scala
val makePopularService =
  defer:
    val cloudStorage =
      ZIO.service[CloudStorage].run
    PopularService(cloudStorage.retrieve)
```

To construct a `PopularService`, we give it the effect that looks up content.
In this version, it goes directly to the `CloudStorage` provider.

Suppose each request to our `CloudStorage` provider costs one dollar.

```scala
def run =
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makePopularService)
  )
```

Output:

```shell
Result: Amount owed: $100
```

The invoice is 100 dollars because every single request reached our `CloudStorage` provider.

Now let's construct a `PopularService` that uses a cache:

```scala
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

```scala
def run =
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makeCachedPopularService)
  )
```

Output:

```shell
Result: Amount owed: $1
```

The invoice is only 1 dollar, because only one request reached the `CloudStorage` provider.
Wonderful!
In practice, the savings will rarely be *this* extreme, but it is reassuring to know we can handle these situations with ease.

## Staying under rate limits

Rate limits are a common way to structure agreements between services.
In the worst case, going above this limit could overwhelm the service and make it crash.
At the very least, you will be charged more for exceeding it.


Defining your rate limiter requires only the 2 pieces of information that should be codified in your service agreement:

```psuedo
$maxRequests / $interval
```

```scala
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

```scala
def run =
  defer:
    val rateLimiter =
      makeRateLimiter.run
    rateLimiter:
      expensiveApiCall
    .timedSecondsDebug:
       s"called API"
    .repeatN(2) // Repeats as fast as allowed
    .timedSecondsDebug("Result")
    .run
```

Output:

```shell
called API [took 0s]
called API [took 1s]
called API [took 1s]
Result [took 2s]
```

Most impressively, we can use the same `RateLimiter` across our application.
No matter the different users/features trying to hit the same resource, they will all be limited such that the entire application respects the rate limit.

```scala
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
          .repeatN(2) // Repeats as fast as allowed
      .timedSecondsDebug:
        "Total time"
      .unit // ignores the list of unit
      .run
```

Output:

```shell
Bill called API [took 0s]
James called API [took 1s]
Bill called API [took 2s]
Bruce called API [took 3s]
James called API [took 3s]
Bill called API [took 3s]
Bruce called API [took 3s]
James called API [took 3s]
Bruce called API [took 2s]
Total time [took 8s]
```

## Constraining concurrent requests

If we want to ensure we don't accidentally DDOS a service, we can restrict the number of concurrent requests to it.


First, we demonstrate the unrestricted behavior:

```scala
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

Output:

```shell
Delicate Resource constructed.
Do not make more than 3 concurrent requests!
Current requests: List(557)
Current requests: List(568, 557)
Current requests: List(180, 568, 557)
Current requests: List(276, 180, 568, 557)
Current requests: List(297, 276, 180, 568, 557)
Result: Crashed the server!!
```

We execute too many concurrent requests, and crash the server.
To prevent this, we need a `Bulkhead`.

```scala
import nl.vroste.rezilience.Bulkhead
val makeOurBulkhead =
  Bulkhead.make(maxInFlightCalls =
    3
  )
```

Next, we wrap our original request with this `Bulkhead`.

```scala
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
  .provide(DelicateResource.live, Scope.default)
```

Output:

```shell
Delicate Resource constructed.
Do not make more than 3 concurrent requests!
Current requests: List(193)
Current requests: List(717, 193)
Current requests: List(927, 717, 193)
Current requests: List(965)
Current requests: List(161, 965)
Current requests: List(737, 161, 965)
Current requests: List(222)
Current requests: List(506, 222)
Current requests: List(987, 506, 222)
Current requests: List(29)
Result: All Requests Succeeded
```

With this small adjustment, we now have a complex, concurrent guarantee.

## Circuit Breaking

Often, when a request fails, it is reasonable to immediately retry.
However, if we aggressively retry in an unrestricted way, we might actually make the problem worse by increasing the load on the struggling service.
Ideally, we would allow some number of aggressive retries, but then start blocking additional requests until the service has a chance to recover.


In this scenario, we are going to repeat our call many times in quick succession.

```scala
val repeatSchedule =
  Schedule.recurs(140) &&
    Schedule.spaced(50.millis)
```

When unrestrained, the code will let all the requests through to the degraded service.

```scala
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

Output:

```shell
Result: Calls made: 141
```

Now we will build our `CircuitBreaker`

```scala
import nl.vroste.rezilience.{
  CircuitBreaker,
  TrippingStrategy,
  Retry
}

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

```scala
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

Output:

```shell
Result: Calls prevented: 75 Calls made: 66
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


```scala
def run =
  defer:
    val contractBreaches =
      Ref.make(0).run

    val req =
      defer:
        val hedged =
          logicThatSporadicallyLocksUp.race:
            logicThatSporadicallyLocksUp
              .delay:
                25.millis

        val duration =
          hedged.run
        if (duration > 1.second)
          contractBreaches.update(_ + 1).run

    ZIO
      .foreachPar(List.fill(50_000)(())):
        _ => req // TODO james still hates this and maybe a collectAllPar could do the trick but we've already wasted 321 hours on this
      .run

    contractBreaches
      .get
      .debug("Contract Breaches")
      .run
```

Output:

```shell
Contract Breaches: 0
Result: 0
```

## Test Reliability

{{ TODO: Intro. Talk about many TestAspects. But then transition to the 2 most common ones (timeout & flaky) }}

### Test Timeouts

{{ TODO: Code example }}

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

### Flaky Tests

{{ TODO: Code example }}

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
