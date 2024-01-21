# Superpowers with Effects

TODO: A couple sentences about the superpowers


## Building a Resilient Process in stages
We want to evolve our process from a simple happy path to a more resilient process.
Progressive enhancement through adding capabilities

TODO: Is "Step" necessary? Some other word?

## Step 1: Happy Path

```scala
runDemo:
  saveUser:
    "mrsdavis"
// User saved
```


## Step 2: Provide Error Fallback Value

```scala
object DatabaseError
object TimeoutError
```

```scala
runDemo:
  saveUser:
    "Robert'); DROP TABLE USERS"
  .orElseFail:
    "ERROR: User could not be saved"
// DatabaseError
// ERROR: User could not be saved
```

## Step 3: Retry Upon Failure


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

## Step 4: Fallback after multiple failures


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
// DatabaseError
// DatabaseError
// ERROR: User could not be saved
```

## Step 5: Timeouts



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

## Step 6: Fallback Effect


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
    .orElseSucceed: // TODO Delete?
      "ERROR: User could not be saved, even to the fallback system"
// DatabaseError
// DatabaseError
// DatabaseError
// DatabaseError
// User sent to manual setup queue
```

## Step 7: Concurrently Execute Effect 


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


## Step 8: Ignore failures in Concurrent Effect 

Feeling a bit "meh" about this step.

```scala
// concurrently save & send analytics, ignoring analytics failures
runDemo:
  // TODO Consider how to dedup strings
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

## Step 9: Rate Limit TODO 


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/03_Superpowers.md)
