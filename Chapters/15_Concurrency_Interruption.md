# Concurrency Interruption

## Why Interruption Is Necessary Throughout the Stack
In order for the `Runtime`  to operate and provide the super powers of `ZIO`, it needs to be able to interrupt running workflows without resource leaks.

## Timeout
## Race

## .withFinalizer
## .zipWithPar
## .aquireRelease effects are uninterruptable
## .fromFutureInterrupt

## Uninterruptable


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

## Future Cancellation

We show that Future's are killed with finalizers that never run

```scala mdoc
import scala.concurrent.Future

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
