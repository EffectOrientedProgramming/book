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

```scala mdoc
object DatabaseError
object TimeoutError
```

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