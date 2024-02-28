# Superpowers with Effects

Effects enable us to progressively add capabilities to increase its reliability and control the unpredictable aspects.
In this chapter you will see that once we've defined parts of a program in terms of Effects
  , we gain some superpowers.
The reason we call it "superpowers" is that the capabilities you will see can be attached to **any** Effect.
For recurring concerns in our program, we do not want to create a bespoke solution for each context.
To illustrate this we will show a few examples of common capabilities applied to Effects.
Let's start with the "happy path" where we save a user to a database 
  (an Effect) 
  and then gradually add superpowers.

```scala mdoc:invisible
object HiddenPrelude:
  enum Scenario:
    case HappyPath
    case NeverWorks
    case NumberOfSlowCall(ref: Ref[Int])
    case WorksOnTry(attempts: Int, ref: Ref[Int])

  import zio.Runtime.default.unsafe
  val invocations =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe =
        u
      unsafe
        .run(
          Ref.make[Scenario](
            Scenario.HappyPath
          )
        )
        .getOrThrowFiberFailure()
    )

  def resetScenario(scenario: Scenario) =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe =
        u
      unsafe
        .run(invocations.set(scenario))
        .getOrThrowFiberFailure()
    )

  object Scenario:
    val FirstIsSlow =
      Unsafe.unsafe { implicit unsafe =>
        NumberOfSlowCall(
          Runtime
            .default
            .unsafe
            .run(Ref.make(0))
            .getOrThrow()
        )
      }

    val DoesNotWorkInitially =
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

  def runScenario[E, A](s: Scenario)(
      z: => ZIO[Scope, E, A]
  ): Unit =
    Unsafe.unsafe { (u: Unsafe) =>
      given Unsafe =
        u
      val res =
        unsafe
          .run(
            Rendering
              .renderEveryPossibleOutcomeZio(
                defer:
                  invocations.set(s).run
                  z.run
                .provide(Scope.default)
              )
              .withConsole(OurConsole)
          )
          .getOrThrowFiberFailure()
      // This is the *only* place we can trust to
      // always print the final value
      println(res)
    }

  def saveUser(username: String) =
    val succeed =
      ZIO.succeed("User saved")
    val fail =
      ZIO
        .fail("**Database crashed!!**")
        .tapError { error =>
          ZIO.succeed:
            println(error)

          // TODO This blows up, probably due to
          // our general ZIO Console problem.
//          Console
//            .printLineError("Database Error")
//            .orDie
        }

    defer {
      invocations.get.run match
        case Scenario.HappyPath =>
          succeed.run
        case Scenario.NeverWorks =>
          fail.run

        case scenario: Scenario.NumberOfSlowCall =>
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

  def sendToManualQueue(username: String) =
    ZIO
      .attempt("User sent to manual setup queue")

  def userSignupInitiated(username: String) =
    ZIO.succeed(
      println(s"Signup initiated for $username")
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

  // TODO Decide how much to explain this in the
  // prose,
  // without revealing the implementation
  extension [R, E, A](z: ZIO[R, E, A])
    def fireAndForget(
        background: ZIO[R, Nothing, Any]
    ) =
      z.zipParLeft(background.forkDaemon)

end HiddenPrelude

import HiddenPrelude.*
import Scenario.*
```

## Effect 1. The Happy Path is Only One Path

To start with we save a user to a database:

```scala mdoc:silent
val userName =
  "Morty"
```

```scala mdoc:silent
val effect1 =
  saveUser:
    userName
```

This `val` contains the logic of the Effect.
The Effect does not execute until we explicitly run it.

```scala mdoc
runScenario(HappyPath):
  effect1
```

`runScenario(HappyPath)` runs our Effect in the "happy path" so that it will not fail.
This allows us to simulate failure scenarios in the next examples.

In real systems, assuming the "happy path" causes strange errors for users because the errors are unhandled.

## Effect 2. What if Failure is Temporary?

We can also run `effect1` in a scenario that will cause it to fail.

```scala mdoc
runScenario(DoesNotWorkInitially):
  effect1
```

Sometimes things work when you keep trying.  
We can attach a `retry` to our first Effect.
`retry` accepts a `Schedule` that determines when to retry.

```scala mdoc:silent
import Schedule.{recurs, spaced}
val effect2 =
  effect1.retry:
    // TODO Restore 1.second when done editing
    recurs(3) && spaced(1.milli)
```

The Effect with the retry behavior becomes a new Effect.
`recurs(3)` builds a `Schedule` that happens 3 times.
`spaced(1.second)` is a `Schedule` that happens once per second, forever.
By combining them, we get a `Schedule` that does something only 3 times and once per second.
Schedules can be applied to many different capabilities.
We do this because we assume the failure will likely be resolved within 3 seconds.

```scala mdoc
runScenario(DoesNotWorkInitially):
  effect2
```

The output shows that running the Effect failed twice trying to save the user, then it succeeded.

### What if it never succeeds?

```scala mdoc
runScenario(NeverWorks):
  effect2
```

In the `NeverWorks` scenarios, the Effect failed its initial attempt, and failed the subsequent three retries.  
It eventually returns the DB error to the user.

## Effect 3. Users like nice error messages

Let's handle the error and return something nicer:

```scala mdoc:silent
val effect3 =
  effect2.orElseFail:
    "ERROR: User could not be saved"
```

```scala mdoc
runScenario(NeverWorks):
  effect3
```

**Any** fallible Effect can attach a variety of error handling capabilities.
`orElseFail` transforms any failure into a user-friendly form.
We added the capability without restructuring the original Effect.
This is just one way to handle errors.
ZIO provides many variations, which we will not cover exhaustively.

The `orElseFail` is combined with the first (`retry`) creating another new Effect that has both error handling capabilities.
Like `retry`, the `orElseFail` can be added to any fallible Effect.

Not only can capabilities be added to any Effect, Effects can be combined and modified, producing new Effects.

## Effect 4. Things Can Take a Long Time

```scala mdoc:silent
val effect4 =
  effect3
    .timeoutFail("Save timed out"):
      5.seconds
```

If the effect does not complete within 5 seconds, it fails.
Like the other capabilities for error handling, timeouts can be added to any Effect.

```scala mdoc
runScenario(FirstIsSlow):
  effect4
```

Running the new Effect in the `FirstIsSlow` scenario causes it to take longer than the 5 second timeout.

## Effect 5. Fallback From Failure

In some cases there may be a fallback for a failed Effect.

```scala mdoc:silent
val effect5 =
  effect4.orElse:
    sendToManualQueue:
      userName
```

The `orElse` creates a new Effect with a fallback.  The `sendToManualQueue` simulates alternative fallback logic.

```scala mdoc
// fails - with retry and fallback
runScenario(NeverWorks):
  effect5
```

We run the effect again in the `NeverWorks` scenario
  , causing it to execute the fallback Effect.

## Effect 6. Concurrent Execution

Effects can be run concurrently and as an example, we can at the same time as the user is being saved, send an event to another system.

```scala mdoc:silent
val effect6 =
  effect5.fireAndForget:
    userSignupInitiated:
      userName
```

`fireAndForget` is a convenience method we defined (in hidden code) that makes it easy to run two effects in parallel and ignore any failures on the `userSignupInitiated` Effect.
We can add all sorts of custom behavior to our Effect type, and then invoke them regardless of error and result types.

```scala mdoc
runScenario(HappyPath):
  effect6
```

## Effect 7. Timing all of this

```scala mdoc:silent
val effect7 =
  effect6.timed
```

```scala mdoc
runScenario(HappyPath):
  effect7
```

## Effect 8. Maybe we don't want this to run at all?

Now that we have added all of these superpowers to our process
  , our lead engineer lets us known that a certain user should be prevented from using our system.

```scala mdoc:silent
val effect8 =
  effect7.when(userName != "Morty")
```

```scala mdoc
runScenario(HappyPath):
  effect8
```
We can add behavior to the end of our complex Effect
  , that prevents it from ever executing in the first place.

## Uniformity
Because we have chosen such a powerful Effect type
  , we can add freely all of these capabilities to any Effect.
