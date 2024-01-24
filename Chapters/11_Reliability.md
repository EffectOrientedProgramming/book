# Resilience

## Caching



## Staying under rate limits
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
      val res = Random.nextIntBounded(1000).run

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
```

```scala mdoc
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
```

## Circuit Breaking
