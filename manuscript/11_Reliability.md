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
    ZIO
      .foreachPar(1 to 10): _ =>
        //          bulkhead:
        ZIO.serviceWithZIO[DelicateResource](
          _.request
        )
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
    ZIO
      .foreachPar(1 to 10): _ =>
        bulkhead:
          ZIO.serviceWithZIO[DelicateResource](
            _.request
          )
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



## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/11_Reliability.md)
