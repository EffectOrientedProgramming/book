# Reliability

## Caching


One way to achieve better reliability is with caching.

```scala
val thunderingHerdsScenario =
  defer:
    val popularService =
      ZIO.service[PopularService].run

    ZIO
      .foreachPar(List.fill(100)(())): _ =>  // james don't like
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


## Hedging



```scala
runDemo:
  defer:
    val contractBreaches = Ref.make(0).run

    ZIO
      .foreachPar(List.fill(50_000)(())): _ => // james still hates this
        defer:
          val hedged =
            logicThatSporadicallyLocksUp.race:
              logicThatSporadicallyLocksUp.delay:
                25.millis

          // TODO How do we make this demo more obvious?
          //   The request is returning the hypothetical runtime, but that's
          //   not clear from the code that will be visible to the reader.
          val duration = hedged.run
          if (duration > 1.second)
            contractBreaches
              .update(_ + 1)
              .run
      .run

    contractBreaches
      .get
      .debug("Contract Breaches")
      .run
// 0
```

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/11_Reliability.md)
