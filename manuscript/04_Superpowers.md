# Superpowers with Effects


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/04_Superpowers.md)


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
TODO Consider deleting. Uses an extension


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


TODO Slot these in:

### Time
#### Measuring Time
Since there is already a `.timed` method available directly on `ZIO` instances, it might seem redundant to have a `timed` `TestAspect`.
However, they are distinct enough to justify their existence.
`ZIO`s `.timed` methods changes the result type of your code by adding the duration to a tuple in the result.
This is useful, but requires the calling code to handle this new result type.
`TestAspect.timed` is a non-invasive way to measure the duration of a test.
The timing information will be managed behind the scenes, and printed in the test output, without changing any other behavior.

#### Restricting Time
Sometimes, it's not enough to simply track the time that a test takes.
If you have specific Service Level Agreements (SLAs) that you need to meet, you want your tests to help ensure that you are meeting them.
However, even if you don't have contracts bearing down on you, there are still good reasons to ensure that your tests complete in a timely manner.
Services like GitHub Actions will automatically cancel your build if it takes too long, but this only happens at a very coarse level.
It simply kills the job and won't actually help you find the specific test responsible.

A common technique is to define a base test class for your project that all of your tests extend.
In this class, you can set a default upper limit on test duration.
When a test violates this limit, it will fail with a helpful error message.

This helps you to identify tests that have completely locked up, or are taking an unreasonable amount of time to complete.

For example, if you are running your tests in a CI/CD pipeline, you want to ensure that your tests complete quickly, so that you can get feedback as soon as possible.
you can use `TestAspect.timeout` to ensure that your tests complete within a certain time frame.

### Flakiness
Commonly, as a project grows, the supporting tests become more and more flaky.
This can be caused by a number of factors:

- The code is using shared, live services
  Shared resources, such as a database or a file system, might be altered by other processes.
  These could be other tests in the project, or even unrelated processes running on the same machine.

- The code is not thread safe
  Other processes running simultaneously might alter the expected state of the system.

- Resource limitations
  A team of engineers might be able to successfully run the entire test suite on their personal machines.
  However, the CI/CD system might not have enough resources to run the tests triggered by everyone pushing to the repository.
  Your tests might be occasionally failing due to timeouts or lack of memory.
