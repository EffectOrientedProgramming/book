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

## Fork Interruption
Interruption is explicit in the previous situations, but there is an implicit interruption point that you should be aware of.
If an operation is forked, and we exit the scope that created it without joining, then it will be interrupted.

```scala mdoc
runDemo:
  defer:
    ZIO.debug:
      "About to sleep forever"
    .run
    ZIO.sleep:
      Duration.Infinity
    .onInterrupt:
      ZIO.succeed:
        // More mdoc console weirdness :(
        println: 
          "Interrupted the eternal sleep"
    .fork
    .run
```

## Uninterruptable
## .acquireRelease effects are uninterruptible
There are certain cases where you want to ensure code is not interrupted.
For example, when you have a finalizer that needs to free up resources, you need to ensure it completes.



## Future Cancellation (Contra-example. Not necessary for happy path explanation)

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

### .fromFutureInterrupt
