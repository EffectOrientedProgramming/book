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
    case WorksFirstTime
    case NeverWorks
    case FirstIsSlow(ref: Ref[Int])
    case WorksOnTry(attempts: Int, ref: Ref[Int])

  import zio.Runtime.default.unsafe
  val invocations =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe =
        u
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
      given Unsafe =
        u
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

## Effect Example 1. The Happy Path is Only One Path

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

In a real system this gives our users strange errors because they are unhandled.

## Effect Example 2. What if Failure is Temporary?

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

You can see from the output that we failed twice trying to save the user, then it succeeded.

### What if it never succeeds?

```scala mdoc
runScenario(NeverWorks):
  effect2
```

In the `NeverWorks` scenarios, the Effect failed its initial attempt, and failed the subsequent three retries.  
It eventually returns the DB error to the user.

## Effect Example 3. Users like nice error messages

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

The first superpower is that **any** fallible effect can attach a variety of error handling capabilities.
`orElseFail` transforms any failure into a user-friendly form.
We added the capability without restructuring the original effect.
This is just one way to handle errors.
ZIO provides many variations, which we will not cover exhaustively.

The second superpower (`orElseFail`) is combined with the first (`retry`).
Like `retry`, it can be added to any fallible effect.
The uber-superpower is that superpowers can be combined;
this creates a new effect that is the combination of the original effect AND the superpowers applied to it.


> TODO Holy shit moment callout (this is really important)

## Effect Example 4. Things Can Take a Long Time

```scala mdoc:silent
val effect4 =
  effect3.timeoutFail("Took too long to save"):
    // TODO Restore real value when done editing
    5.millis
//      5.seconds
```

```scala mdoc
runScenario(firstIsSlow):
  effect4
```

## Effect Example 5. Fallback From Failure

```scala mdoc:silent
val effect5 =
  effect4.orElse:
    sendToManualQueue:
      userName
```
```scala mdoc
// fails - with retry and fallback
runScenario(NeverWorks):
  effect5
```

## Effect Example 6. Concurrent Execution

`fireAndForget` is an extension method, whose implementation we are hiding for now.

```scala mdoc:silent
val effect6 =
  effect5.fireAndForget:
    userSignupInitiated:
      userName
```

It executes the new Effect in parallel, so even though we have added it "to the end" of our larger existing Effect,
  it completes first.
We can add all sorts of custom behavior to our Effect type, and then invoke them regardless of error and result types.

```scala mdoc
runScenario(HappyPath):
  effect6
```

## Effect Example 7. Timing all of this

```scala mdoc:silent
val effect7 =
  effect6.timed
```

```scala mdoc
runScenario(HappyPath):
  effect7
```

## Effect Example 8. Maybe we don't want this to run at all?

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
