# Concurrency Interruption

## Why Interruption Is Necessary Throughout the Stack

## Timeout
## Race

## .withFinalizer
## .zipWithPar
## .aquireRelease effects are uninterruptable
## .fromFutureInterrupt

## Uninterruptable


```scala
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

```scala
runDemo:
  sleepThenPrint:
    2.seconds
  .race:
    sleepThenPrint:
      1.seconds
// 1 s elapsed
// PT1S
```

## Future Cancellation

We show that Future's are killed with finalizers that never run

```scala
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
// Starting operation
// None
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/15_Concurrency_Interruption.md)
