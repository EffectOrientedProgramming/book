# Superpowers

1. The superpowers are the strategies to deal with unpredictability 
1. OpeningHook (1-6)
    1. Note: Progressive enhancement through adding capabilities 
1. Concurrency
    1. Race
    1. Hedge (to show the relationship to OpeningHook ie progressive enhancement)
1. Sequential
    1. ZIO Direct
    1. Note: Combine OpeningHook & Concurrency with ZIO Direct
1. And so much more
    1. Note: And there are many capabilities you might want to express. In the future we will dive into these other capabilities.

- Racing
- Timeout
- Error-handling
  - Fallback
  - Retry
- Repeat
- Parallelism
- Resource Safety
- Mutability that you can trust
- Human-readable
- Cross-cutting Concerns / Observability / Regular Aspects
  - timed
  - metrics
  - debug
  - logging

# Underlying
- Composability
- Success VS failure
- Interruption/Cancellation
- Fibers
- Processor Utilization
  - Fairness
  - Work-stealing
- Resource Control/Management
- Programs as values

```scala
object DatabaseError
object TimeoutError
```


```scala
// works
runDemo:
  saveUser:
    "mrsdavis"
// User saved
```


```scala
// fails
runDemo:
  saveUser:
    "mrsdavis"
  .orElseSucceed:
    "ERROR: User could not be saved"
// DatabaseError
// ERROR: User could not be saved
```



```scala
import zio.Schedule.*
```


```scala
val aFewTimes =
  // TODO Restore original spacing when done
  // editing
  // recurs(3) && spaced(1.second)
  recurs(3)
```

```scala
// fails first time - with retry
runDemo:
  saveUser:
    "morty"
  .retry:
    aFewTimes
  .orElseSucceed:
    "ERROR: User could not be saved"
// DatabaseError
// User saved
```


```scala
// fails every time - with retry
runDemo:
  saveUser:
    "morty"
  .retry:
    aFewTimes
  .orElseSucceed:
    "ERROR: User could not be saved, despite multiple attempts"
// DatabaseError
// DatabaseError
// DatabaseError
// DatabaseError
// TODO Handle long line. 
// Truncating for now: 
// ERROR: User could not be saved, despite multiple attempts
// ERROR: User could not be saved, despite multip
```


```scala
// TODO Restore real value when done editing
val timeLimit = 5.millis
// timeLimit: Duration = PT0.005S
  //  5.seconds

// first is slow - with timeout and retry
runDemo:
  saveUser:
    "morty"
  .timeoutFail(TimeoutError)(timeLimit)
    .retry:
      aFewTimes
    .orElseSucceed:
      "ERROR: User could not be saved"
// Interrupting slow request
// Database Timeout
// User saved
```


```scala
// fails - with retry and fallback
runDemo:
  saveUser:
    "morty"
  .timeoutFail(TimeoutError)(timeLimit)
    .retry:
      aFewTimes
    .orElse
  sendToManualQueue:
    "morty"
  .orElseSucceed:
    "ERROR: User could not be saved, even to the fallback system"
// User sent to manual setup queue
```


```scala
// concurrently save & send analytics
runDemo:
  saveUser:
    "morty"
  .timeoutFail(TimeoutError)(timeLimit)
    .retry(aFewTimes)
    .orElse:
      sendToManualQueue:
        "morty"
    .orElseSucceed:
      "ERROR: User could not be saved"
      // todo: maybe this hidden extension method
      // goes too far with functionality that
      // doesn't really exist
    .fireAndForget:
      userSignupInitiated:
        "morty"
// User saved
```


```scala
// concurrently save & send analytics, ignoring analytics failures
runDemo:
  // TODO Consider ways to dedup morty
  // string
  saveUser:
    "mrsdavis"
  .timeoutFail(TimeoutError)(timeLimit)
    .retry:
      aFewTimes
    .orElse:
      sendToManualQueue:
        "mrsdavis"
    .tapBoth(
      error =>
        userSignUpFailed("mrsdavis", error),
      success =>
        userSignupSucceeded("mrsdavis", success)
    )
    .orElseSucceed:
      "ERROR: User could not be saved"
// Analytics sent for signup completion
// User saved
```

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/03_Superpowers.md)
