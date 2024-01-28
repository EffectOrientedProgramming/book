# Reliability

## Caching

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
      val count = requests.get.run

      "Amount owed: $" + count

object FSLive:
  val hardcodedContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )
end FSLive

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

One way to achieve better reliability is with caching.

```scala mdoc:silent
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

```scala mdoc
runDemo:
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(PopularService.make)
  )
```

```scala mdoc
runDemo:
  thunderingHerdsScenario.provide(
    CloudStorage.live,
    ZLayer.fromZIO(PopularService.makeCached)
  )
```

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
```

## Circuit Breaking


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

```