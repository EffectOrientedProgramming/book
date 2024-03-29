# Reliability


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/10_Reliability.md)


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


To demonstrate, we will take one of the worst case scenarios that your service might encounter: the thundering herd problem.

```scala
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

```scala
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

```scala
runDemo:
  thunderingHerdsScenario
    .provide(CloudStorage.live, popularService)
// Result: Amount owed: $100
```

We can see that the invoice is 100 dollars, because every single request reached our `CloudStorage` provider.

Now we will apply our cache:

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

- building our cache with sensible values
- then passing the `Cache#get` method to our `PopularService` constructor, rather than the bare `CloudStorage#retrieve` method

Now when we run the same scenario, with our cache in place:

```scala
runDemo:
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(makeCachedPopularService)
  )
// Result: Amount owed: $1
```

We can see that the invoice is only 1 dollar, because only one request reached our `CloudStorage` provider.
Wonderful!
In practice, the savings will rarely be *this* extreme, but it is a reassuring to know that we can handle these situations with ease, maintaining a low cost.

## Staying under rate limits


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

```scala
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

```scala
runDemo:
  defer:
    val rateLimiter =
      makeRateLimiter.run
    rateLimiter
      .makeCalls:
        "System"
      .timedSecondsDebug("Result")
      .run
// System called API [took 0s]
// System called API [took 1s]
// System called API [took 1s]
// Result [took 2s]
// Result: ()
```

```scala
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
// Bruce called API [took 0s]
// Bill called API [took 1s]
// James called API [took 2s]
// Bruce called API [took 3s]
// Bill called API [took 3s]
// James called API [took 3s]
// Bruce called API [took 3s]
// Bill called API [took 3s]
// James called API [took 3s]
// Total time [took 8s]
// Result: List((), (), ())
```

## Constraining concurrent requests
If we want to ensure we don't accidentally DDOS a service, we can restrict the number of concurrent requests to it.



```scala
runDemo:
  defer:
    val delicateResource =
      ZIO.service[DelicateResource].run
    ZIO
      .foreachPar(1 to 10):
        _ =>
          //          bulkhead:
          delicateResource.request
      .as("All Requests Succeeded!")
      .run
  .provideSome[Scope]:
    DelicateResource.live
// Delicate Resource constructed.
// Do not make more than 3 concurrent requests!
// Result: Killed the server!!
```

```scala
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
      .foreachPar(1 to 10):
        _ =>
          bulkhead:
            delicateResource.request
      .as("All Requests Succeeded")
      .run
  .provideSome[Scope]:
    DelicateResource.live
// Delicate Resource constructed.
// Do not make more than 3 concurrent requests!
// Result: All Requests Succeeded
```

## Circuit Breaking


```scala
val repeatSchedule =
  Schedule.recurs(140) &&
    Schedule.spaced(50.millis)
```

```scala
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
// Result: Calls made: 141
```

```scala
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

```scala
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
// Result: Calls prevented: 74 Calls made: 67
```

## Hedging



```scala
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
// Result: 0
```

#### Restricting Time
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

### Flakiness
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
