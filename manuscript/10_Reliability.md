# Reliability


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/10_Reliability.md)


## Caching


One way to achieve better reliability is with caching.

```scala
val thunderingHerdsScenario =
  defer:
    val popularService =
      ZIO.service[PopularService].run

    ZIO
      .foreachPar(List.fill(100)(())):
        _ => // james don't like
          popularService.retrieve:
            Path.of("awesomeMemes")
      .run

    val cloudStorage =
      ZIO.service[CloudStorage].run

    cloudStorage.invoice.debug.run

object PopularService:
  private def lookup(key: Path) =
    defer:
      val cloudStorage =
        ZIO.service[CloudStorage].run

      cloudStorage.retrieve(key).run

  val make =
    defer:
      val cloudStorage =
        ZIO.service[CloudStorage].run
      PopularService(cloudStorage.retrieve)

  val makeCached =
    defer:
      val cache =
        Cache
          .make(
            capacity = 100,
            timeToLive = Duration.Infinity,
            lookup = Lookup(lookup)
          )
          .run

      PopularService(cache.get)
end PopularService
```

```scala
runDemo:
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(PopularService.make)
  )
// Amount owed: $100
```

```scala
runDemo:
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(PopularService.makeCached)
  )
// Amount owed: $1
```

## Staying under rate limits


```scala
import nl.vroste.rezilience.RateLimiter

val makeRateLimiter =
  RateLimiter.make(max = 1, interval = 1.second)
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
    val rateLimiter = makeRateLimiter.run
    rateLimiter
      .makeCalls:
        "System"
      .timedSecondsDebug("Result").run
// System called API [took 0s]
// System called API [took 1s]
// System called API [took 1s]
// Result [took 2s]
// ()
```

```scala
runDemo:
  defer:
    val rateLimiter = makeRateLimiter.run
    val people = List("Bill", "Bruce", "James")

    ZIO
      .foreachPar(people):
        rateLimiter.makeCalls
      .timedSecondsDebug:
        "Total time"
      .run
// Bruce called API [took 0s]
// James called API [took 1s]
// Bill called API [took 2s]
// Bruce called API [took 3s]
// James called API [took 3s]
// Bill called API [took 3s]
// Bruce called API [took 3s]
// James called API [took 3s]
// Bill called API [took 3s]
// Total time [took 8s]
// List((), (), ())
```

## Constraining concurrent requests
If we want to ensure we don't accidentally DDOS a service, we can restrict the number of concurrent requests to it.



```scala
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
// Delicate Resource constructed.
// Do not make more than 3 concurrent requests!
// Killed the server!!
```

```scala
import nl.vroste.rezilience.Bulkhead

runDemo:
  defer:
    val bulkhead: Bulkhead =
      Bulkhead.make(maxInFlightCalls = 3).run
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
// All Requests Succeeded
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
    val numCalls = Ref.make[Int](0).run

    externalSystem(numCalls)
      .ignore
      .repeat(repeatSchedule)
      .run

    val made = numCalls
      .get
      .run

    s"Calls made: $made"
// Calls made: 141
```

```scala
import nl.vroste.rezilience.{CircuitBreaker, TrippingStrategy, Retry}
import nl.vroste.rezilience.CircuitBreaker.CircuitBreakerOpen

val makeCircuitBreaker =
  CircuitBreaker.make(
    trippingStrategy =
      TrippingStrategy
        .failureCount(maxFailures = 2),
    resetPolicy = Retry.Schedules.common(),
  )
```

```scala
runDemo:
  defer:
    val cb = makeCircuitBreaker.run
    val numCalls = Ref.make[Int](0).run
    val numPrevented = Ref.make[Int](0).run
    val protectedCall =
      cb(externalSystem(numCalls))
        .catchSome:
          case CircuitBreakerOpen =>
            numPrevented.update(_ + 1)
  
    protectedCall
      .ignore
      .repeat(repeatSchedule)
      .run

    val prevented = 
      numPrevented
        .get
        .run

    val made =
      numCalls
        .get
        .run
    s"Calls prevented: $prevented Calls made: $made"
// Calls prevented: 74 Calls made: 67
```

## Hedging



```scala
runDemo:
  defer:
    val contractBreaches = Ref.make(0).run

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
            val duration = hedged.run
            if (duration > 1.second)
              contractBreaches.update(_ + 1).run
      .run

    contractBreaches
      .get
      .debug("Contract Breaches")
      .run
// 0
```

