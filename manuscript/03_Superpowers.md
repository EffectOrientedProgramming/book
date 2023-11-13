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
- Resource Safety
- Mutability that you can trust
- Human-readable
- Cross-cutting Concerns / Observability / Regular Aspects
  - timed
  - metrics
  - debug
  - logging

- Interruption/Cancellation
- Fibers
- Processor Utilization
  - Fairness
  - Work-stealing
- Resource Control/Management

```scala
object DatabaseError
object TimeoutError
```


## Building a Resilient Process in stages

### Successful Code

```scala
// works
runDemo:
  saveUser:
    "mrsdavis"
// User saved
```


### Error Fallback Value

```scala
// fails
runDemo:
  saveUser:
    "mrsdavis"
  .orElseFail:
    "ERROR: User could not be saved"
// DatabaseError
// ERROR: User could not be saved
```

### Retry Upon Failure


```scala
import zio.Schedule.{recurs, spaced}
val aFewTimes =
  // TODO Restore original spacing when done
  // editing
  // recurs(3) && spaced(1.second)
  recurs(3) && spaced(1.millis)
```

```scala
runDemo:
  saveUser:
    "morty"
  .retry:
    aFewTimes
  .orElseSucceed:
    "ERROR: User could not be saved"
// DatabaseError
// DatabaseError
// User saved
```

### Fallback after multiple failures


```scala
// fails every time - with retry
runDemo:
  saveUser:
    "morty"
  .retry:
    aFewTimes
  .orElseSucceed:
    "ERROR: User could not be saved"
// DatabaseError
// DatabaseError
// DatabaseError
// DatabaseError
// ERROR: User could not be saved
```

### Timeouts



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

### Fallback Effect


```scala
// fails - with retry and fallback
runDemo:
  saveUser:
    "morty"
  .timeoutFail(TimeoutError)(timeLimit)
  .retry:
    aFewTimes
  .orElse:
    sendToManualQueue:
      "morty"
  .orElseSucceed:
    "ERROR: User could not be saved, even to the fallback system"
// DatabaseError
// DatabaseError
// DatabaseError
// DatabaseError
// User sent to manual setup queue
```

### Concurrently Execute Effect 


```scala
// concurrently save & send analytics
runDemo:
  saveUser:
    "morty"
  // todo: maybe this hidden extension method
  // goes too far with functionality that
  // doesn't really exist
  // TODO Should we fireAndForget before the
  // retries/fallbacks?
  .fireAndForget:
    userSignupInitiated:
      "morty"
  .timeoutFail(TimeoutError)(timeLimit)
  .retry:
    aFewTimes
  .orElse:
    sendToManualQueue:
      "morty"
  .orElseSucceed:
    "ERROR: User could not be saved"
// User saved
```


### Ignore failures in Concurrent Effect 

Feeling a bit "meh" about this step.

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
