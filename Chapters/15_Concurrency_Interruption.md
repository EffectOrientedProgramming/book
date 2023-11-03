# Concurrency Interruption

1. Timeout
1. Race

```scala mdoc
// This is duplicate code
def sleepThenPrint(
    d: Duration
): ZIO[Any, java.io.IOException, Duration] =
  defer:
    ZIO
      .sleep:
        d
      .run
    println:
      s"${d.render} elapsed"
    d
```

```scala mdoc
runDemo:
  sleepThenPrint:
    2.seconds
  .race:
    sleepThenPrint:
      1.seconds
```

We show that Future's are killed with finalizers that never run
A:

```scala mdoc
import scala.concurrent.Future
runDemo:
  ZIO
    .fromFuture:
      Future:
        try
          println("Starting operation")
          Thread.sleep(500)
          println("Ending operation")
        finally
          println("Cleanup")
    .timeout:
      25.millis
```

B:

```scala mdoc
runDemo:
  ZIO
    .fromFuture:
      Future:
        try
          println:
            "Starting operation"
          Thread.sleep:
            500
          println:
            "Ending operation"
        finally
          println:
            "Cleanup"
    .timeout:
      25.millis
```
