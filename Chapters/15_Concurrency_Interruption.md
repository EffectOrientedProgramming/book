# Concurrency Interruption

## Why Interruption Is Necessary Throughout the Stack
In order for the `Runtime`  to operate and provide the super powers of `ZIO`, it needs to be able to interrupt running workflows without resource leaks.

## Timeout
Since we can reliably interrupt arbitrary ZIOs, we can attach an upper bound for the runtime of the ZIO.
This does not change the type of the workflow, it _only_ changes the runtime behavior.

## Race
If we have 2 ZIO's that each produce the same types, we can race them, acquiring the first result and cancel the ongoing calculation.
We have taken 2 completely separate workflows and fused them into one.

## .withFinalizer
## .onInterrupt
## .zipWithPar
## Uninterruptable
## .aquireRelease effects are uninterruptable
There are certain cases where you want to ensure code is not interrupted.
For example, when you have a finalizer that needs to free up resources, you need to ensure it completes.
## .fromFutureInterrupt



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
