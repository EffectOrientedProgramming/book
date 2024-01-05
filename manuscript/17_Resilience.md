# Resilience

## Staying under rate limits
## Constraining concurrent requests
If we want to ensure we don't accidentally DDOS a service, we can restrict the number of concurrent requests to it.

```scala
trait DelicateResource:
    val request: ZIO[Any, String, Int]
```


```scala
runDemo:
  defer:
    ZIO
      .foreachPar(1 to 10): _ =>
        //          bulkhead:
        ZIO.serviceWithZIO[DelicateResource](_.request)
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
      Bulkhead
        .make(maxInFlightCalls = 3)
        .run
    ZIO
      .foreachPar(1 to 10): _ =>
        bulkhead:
          ZIO.serviceWithZIO[DelicateResource](_.request)
      .as("All Requests Succeeded")
      .catchAll(err => ZIO.succeed(err))
      .debug
      .run
  .provideSome[Scope]:
    DelicateResource.live
// All Requests Succeeded
```

## Circuit Breaking

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/17_Resilience.md)
