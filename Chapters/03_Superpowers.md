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

```scala mdoc
runDemo(ZIO.debug("Hi"))
```

```scala mdoc
Unsafe.unsafe { implicit unsafe =>
  Runtime
    .default
    .unsafe
    .run(Ref.make(0))
    .getOrThrow()
}
```



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
        .run(Ref.make[Scenario](Scenario.WorksFirstTime))
        .getOrThrowFiberFailure()
    )

  def resetScenario(scenario: Scenario) =
    Unsafe.unsafe((u: Unsafe) =>
        given Unsafe = u
        unsafe
          .run(invocations.set(Scenario.WorksFirstTime))
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

    val doesNotWorkFirstTime =
      Unsafe.unsafe { implicit unsafe =>
        WorksOnTry(
          1,
          Runtime
            .default
            .unsafe
            .run(Ref.make(0))
            .getOrThrow()
        )
      }
  end Scenario

  def saveUser(
      username: String,
      hiddenScenario: Scenario =
        Scenario.WorksFirstTime
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
      hiddenScenario match
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
      .succeed(
        "Analytics sent for signup failure"
      )
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

```scala mdoc
// works
runDemo:
  saveUser:
    "mrsdavis"
```

```scala mdoc
// fails
runDemo:
  saveUser("mrsdavis", Scenario.NeverWorks)
    .orElseSucceed:
      "ERROR: User could not be saved"
```



```scala mdoc
import zio.Schedule.*
```

```scala mdoc
val aFewTimes =
  // TODO Restore original spacing when done editing
  // recurs(3) && spaced(1.second)
  recurs(3)
  
// fails first time - with retry
runDemo:
  saveUser(
    "mrsdavis",
    Scenario.doesNotWorkFirstTime
  ).retry:
    aFewTimes
  .orElseSucceed:
      "ERROR: User could not be saved"
```

```scala mdoc
// fails every time - with retry
runDemo:
  saveUser("mrsdavis", Scenario.NeverWorks)
    .retry:
      aFewTimes
    .orElseSucceed:
      "ERROR: User could not be saved, despite multiple attempts"
```

```scala mdoc
// TODO Restore real value when done editing
val timeLimit = 
  5.millis
  //  5.seconds

// first is slow - with timeout and retry
runDemo:
  saveUser("mrsdavis", Scenario.firstIsSlow)
    .timeoutFail(TimeoutError)(timeLimit)
    .retry:
      aFewTimes
    .orElseSucceed(
      "ERROR: User could not be saved"
    )
```

```scala mdoc
// fails - with retry and fallback
runDemo:
  saveUser("mrsdavis", Scenario.NeverWorks)
    .timeoutFail(TimeoutError)(timeLimit)
    .retry:
      aFewTimes
    .orElse
      sendToManualQueue:
        "mrsdavis"
    .orElseSucceed:
      "ERROR: User could not be saved, even to the fallback system"
```

```scala mdoc
// concurrently save & send analytics
runDemo:
  saveUser("mrsdavis")
    .timeoutFail(TimeoutError)(timeLimit)
    .retry(aFewTimes)
    .orElse:
      sendToManualQueue:
        "mrsdavis"
    .orElseSucceed:
      "ERROR: User could not be saved"
    // todo: maybe this hidden extension method
    // goes too far with functionality that
    // doesn't really exist
    .fireAndForget:
      userSignupInitiated:
        "mrsdavis"
```

```scala mdoc
// concurrently save & send analytics, ignoring analytics failures
runDemo:
  // TODO Consider ways to dedup mrsdavis
  // string
  saveUser("mrsdavis")
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