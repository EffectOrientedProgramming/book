# Superpowers with Effects


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/04_Superpowers.md)


Effects enable us to progressively add capabilities to a program to increase its reliability and control the unpredictable aspects.
In this chapter you will see that once we've defined parts of a program in terms of effects, we gain some superpowers.
The reason we call it "superpowers" is that the capabilities you will see can be attached to **any** effect. For recurring concerns in our program, we do not want to create a bespoke solution for each context.
To illustrate this we will show a few examples of common capabilities applied to effects.
Let's start with the "happy path" where we save a user to a database (an effect) and then gradually add superpowers.


## Example 1. The Happy Path is The Wrong Path

To start with we save a user to a database:

```scala
runDemo:
  saveUser:
    "mrsdavis"
// User saved
```


In a real system this gives our users strange errors because they are unhandled.

## Example 2. Users Like Nice Errors

By handling the error, we can return something nicer:

```scala
runDemo:
  saveUser:
    "Robert'); DROP TABLE USERS"
  .orElseFail:
    "ERROR: User could not be saved"
// **Database crashed!!**
// ERROR: User could not be saved
```

The first superpower is that **any** fallible effect can attach a variety of error handling capabilities.
`orElseFail` transforms any failure into a user-friendly form.
We added the capability without restructuring the original effect.
This is just one way to handle errors.
ZIO provides many variations, which we will not cover exhaustively.

## Example 3. What if Failure is Temporary?


Sometimes things work when you keep trying.  We can use a schedule to determine how to keep trying:

TODO {{runDemo should use a super fast clock so our builds aren't slow}}
```scala
val aFewTimes =
  Schedule.spaced(1.milli) && Schedule.recurs(3)
```

`spaced(1.second)` is a `Schedule` that happens once per second, forever.
`recurs(3)` builds a `Schedule` that happens 3 times.
By combining them, we get a `Schedule` that does something once per second, but only 3 times.
Schedules can be applied to many different capabilities.
We can add lines to the previous example to apply a `retry` to the effect.
We do this because we assume the failure will likely be resolved within 3 seconds:

```scala
runDemo:
  saveUser:
    "morty"
  .retry:     // Added
    aFewTimes // Added
  .orElseFail:
    "ERROR: User could not be saved"
// **Database crashed!!**
// **Database crashed!!**
// User saved
```

You can see from the output that we failed twice trying to save the user, then it succeeded.

The second superpower (`retry`) is combined with the first (`orElseFail`).
Like `orElseFail`, it can be added to any fallible effect.
The uber-superpower is that superpowers can be combined;
  this creates a new effect that is the combination of the original effect AND the superpowers applied to it.

> TODO Holy shit moment callout (this is really important)

## Example 4. Failure is an Option


This uber-super power is further illustrated when the retries do not ultimately succeed:

```scala
runDemo:
  saveUser:
    "morty"
  .retry:
    aFewTimes
  .orElseFail:
    "ERROR: User could not be saved"
// **Database crashed!!**
// **Database crashed!!**
// **Database crashed!!**
// **Database crashed!!**
// ERROR: User could not be saved
```

In this run of the program, the effect failed its initial attempt, and failed the subsequent three retries.  The final failure was handled by the `orElseFail`.

## Example 5. Timeouts


```scala
object TimeoutError
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
    .orElseFail:
      "ERROR: User could not be saved"
// Interrupting slow request
// Database Timeout
// User saved
```

## Example 6. Fallback Effect


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
    .orElseFail: // TODO Delete?
      "ERROR: User could not be saved, even to the fallback system"
// **Database crashed!!**
// **Database crashed!!**
// **Database crashed!!**
// **Database crashed!!**
// User sent to manual setup queue
```

## Example 7. Concurrently Execute Effect 
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
    .orElseFail:
      "ERROR: User could not be saved"
// User saved
```


## Example 8. Ignore failures in Concurrent Effect 

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
    .orElseFail:
      "ERROR: User could not be saved"
// Analytics sent for signup completion
// User saved
```

## Example 9. Rate Limit TODO 

TODO {{Can this be a progressive enhancement or just wait until the reliability chapter?}}


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
