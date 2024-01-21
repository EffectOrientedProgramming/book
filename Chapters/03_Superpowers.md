# Superpowers with Effects

TODO: A couple sentences about the superpowers

```scala mdoc:invisible
object HiddenPrelude:
  enum Scenario:
    case WorksFirstTime
    case NeverWorks
    case FirstIsSlow(ref: Ref[Int])
    case WorksOnTry(attempts: Int, ref: Ref[Int])

  import zio.Runtime.default.unsafe
  val invocations =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe = u
      unsafe
        .run(
          Ref.make[Scenario](
            Scenario.WorksFirstTime
          )
        )
        .getOrThrowFiberFailure()
    )

  def resetScenario(scenario: Scenario) =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe = u
      unsafe
        .run(invocations.set(scenario))
        .getOrThrowFiberFailure()
    )

  object Scenario:
    val firstIsSlow =
      Unsafe.unsafe { implicit unsafe =>
        FirstIsSlow(
          Runtime
            .default
            .unsafe
            .run(Ref.make(0))
            .getOrThrow()
        )
      }

    val doesNotWorkInitially =
      Unsafe.unsafe { implicit unsafe =>
        WorksOnTry(
          2,
          Runtime
            .default
            .unsafe
            .run(Ref.make(0))
            .getOrThrow()
        )
      }
  end Scenario

  def saveUser(
      username: String
  ): ZIO[Any, DatabaseError.type, String] =
    val succeed = ZIO.succeed("User saved")
    val fail =
      ZIO
        .fail(DatabaseError)
        .tapError { _ =>
          ZIO.succeed:
            println("DatabaseError")

          // TODO This blows up, probably due to
          // our general ZIO Console problem.
//          Console
//            .printLineError("Database Error")
//            .orDie
        }

    defer {
      invocations.get.run match
        case Scenario.WorksFirstTime =>
          succeed.run
        case Scenario.NeverWorks =>
          fail.run

        case scenario: Scenario.FirstIsSlow =>
          val numCalls =
            scenario.ref.getAndUpdate(_ + 1).run
          if numCalls == 0 then
            ZIO.never.run
          else
            ZIO
              .succeed:
                println:
                  "Database Timeout"
              .run

            succeed.run

        case Scenario
              .WorksOnTry(attempts, ref) =>
          val numCalls =
            ref.getAndUpdate(_ + 1).run
          if numCalls == attempts then
            succeed.run
          else
            fail.run
    }.onInterrupt(
      ZIO.debug("Interrupting slow request")
    )
  end saveUser

  def sendToManualQueue(
      username: String
  ): ZIO[Any, TimeoutError.type, String] =
    ZIO
      .succeed("User sent to manual setup queue")

  def userSignupInitiated(username: String) =
    ZIO.succeed(
      "Analytics sent for signup initiation"
    )

  def userSignupSucceeded(
      username: String,
      success: String
  ) =
    ZIO
      .succeed(
        "Analytics sent for signup completion"
      )
      .delay(1.millis)
      .debug
      .fork
      .uninterruptible

  def userSignUpFailed(
      username: String,
      error: Any
  ) =
    ZIO
      .succeed:
        "Analytics sent for signup failure"
      .delay(1.millis)
      .debug
      .fork
      .uninterruptible

  extension [R, E, A](z: ZIO[R, E, A])
    def fireAndForget(
        background: ZIO[R, Nothing, Any]
    ): ZIO[R, E, A] =
      z.zipParLeft(background.forkDaemon)

end HiddenPrelude

import HiddenPrelude.*
```

## Building a Resilient Process in stages
We want to evolve our process from a simple happy path to a more resilient process.
Progressive enhancement through adding capabilities

TODO: Is "Step" necessary? Some other word?

## Step 1: Happy Path

```scala mdoc
runDemo:
  saveUser:
    "mrsdavis"
```

```scala mdoc:invisible
HiddenPrelude.resetScenario(Scenario.NeverWorks)
```

## Step 2: Provide Error Fallback Value

```scala mdoc
object DatabaseError
object TimeoutError
```

```scala mdoc
runDemo:
  saveUser:
    "Robert'); DROP TABLE USERS"
  .orElseFail:
    "ERROR: User could not be saved"
```

## Step 3: Retry Upon Failure

```scala mdoc:invisible
HiddenPrelude
  .resetScenario(Scenario.doesNotWorkInitially)
```

```scala mdoc:silent
import zio.Schedule.{recurs, spaced}
val aFewTimes =
  // TODO Restore original spacing when done
  // editing
  // recurs(3) && spaced(1.second)
  recurs(3) && spaced(1.millis)
```

```scala mdoc
runDemo:
  saveUser:
    "morty"
  .retry:
    aFewTimes
  .orElseSucceed:
    "ERROR: User could not be saved"
```

## Step 4: Fallback after multiple failures

```scala mdoc:invisible
HiddenPrelude.resetScenario(Scenario.NeverWorks)
```

```scala mdoc
runDemo:
  saveUser:
    "morty"
  .retry:
    aFewTimes
  .orElseSucceed:
    "ERROR: User could not be saved"
```

## Step 5: Timeouts


```scala mdoc:invisible
HiddenPrelude.resetScenario(Scenario.firstIsSlow)
```

```scala mdoc
// TODO Restore real value when done editing
val timeLimit = 5.millis
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
```

## Step 6: Fallback Effect

```scala mdoc:invisible
HiddenPrelude.resetScenario(Scenario.NeverWorks)
```

```scala mdoc
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
```

## Step 7: Concurrently Execute Effect 

```scala mdoc:invisible
HiddenPrelude
  .resetScenario(Scenario.WorksFirstTime)
```

```scala mdoc
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
```

```scala mdoc:invisible
HiddenPrelude
  .resetScenario(Scenario.WorksFirstTime)
```

## Step 8: Ignore failures in Concurrent Effect 

Feeling a bit "meh" about this step.

```scala mdoc
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
