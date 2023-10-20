# Concurrency Interruption


```scala mdoc
// This is duplicate code
def sleepThenPrint(
    d: Duration
): ZIO[Any, java.io.IOException, Duration] =
  defer {
    ZIO.sleep(d).run
    println(s"${d.render} elapsed")
    d
  }
```

```scala mdoc
runDemo(
  ZIO.raceAll(
    sleepThenPrint(2.seconds),
    Seq(sleepThenPrint(1.seconds))
  )
)
```

We show that Future's are killed with finalizers that never run
```scala mdoc
import scala.concurrent.Future
runDemo(
  ZIO
    .fromFuture:
      Future:
        try Thread.sleep(500)
        finally println("Cleanup")
        "Success!"
    .timeout(25.millis)
)
```
