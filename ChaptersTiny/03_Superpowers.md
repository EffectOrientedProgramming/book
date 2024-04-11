# Superpowers with Effects

Effects enable us to progressively add capabilities to increase reliability and control the unpredictable aspects.
In this chapter you will see that once we've defined parts of a program in terms of Effects, we gain some superpowers.
The reason we call it "superpowers" is that the capabilities you will see can be attached to **any** Effect.
For recurring concerns in our program, we do not want to create a bespoke solution for each context.
To illustrate this we will show a few examples of common capabilities applied to Effects.
Let's start with the "happy path" where we save a user to a database
(an Effect)
and then gradually add superpowers.

To start with we save a user to a database:

```scala mdoc:silent
val userName =
  "Morty"
```

```scala mdoc:invisible
object HiddenPrelude:
  enum Scenario:
    case HappyPath
    case NeverWorks
    case NumberOfSlowCall(ref: Ref[Int])
    case WorksOnTry(attempts: Int, ref: Ref[Int])

  import zio.Runtime.default.unsafe
  val invocations: Ref[Scenario] =
    Unsafe.unsafe(
      (u: Unsafe) =>
        given Unsafe =
          u
        unsafe
          .run(
            Ref
              .make[Scenario](Scenario.HappyPath)
          )
          .getOrThrowFiberFailure()
    )

  def resetScenario(scenario: Scenario) =
    Unsafe.unsafe(
      (u: Unsafe) =>
        given Unsafe =
          u
        unsafe
          .run(invocations.set(scenario))
          .getOrThrowFiberFailure()
    )

  object Scenario:
    val FirstIsSlow =
      Unsafe.unsafe {
        implicit unsafe =>
          NumberOfSlowCall(
            Runtime
              .default
              .unsafe
              .run(Ref.make(0))
              .getOrThrow()
          )
      }

    val DoesNotWorkInitially =
      Unsafe.unsafe {
        implicit unsafe =>
          WorksOnTry(
            3,
            Runtime
              .default
              .unsafe
              .run(Ref.make(0))
              .getOrThrow()
          )
      }
  end Scenario

  def runScenario[E, A](
      scenario: Scenario,
      logic: => ZIO[Scope, E, A]
  ): Unit =
    Unsafe.unsafe {
      (u: Unsafe) =>
        given Unsafe =
          u
        val res =
          unsafe
            .run(
              Rendering
                .renderEveryPossibleOutcomeZio(
                  defer:
//                  val invocations = Ref.make(0).run
                    resetScenario(scenario)
//                  invocations.set(s).run
                    logic.run
                  .provide(Scope.default)
                )
                .withConsole(OurConsole)
            )
            .getOrThrowFiberFailure()
        println("Result: " + res)
    }

  def saveUser(username: String) =
    val succeed =
      ZIO.succeed:
        "User saved"
    val fail =
      ZIO
        .fail:
          "**Database crashed!!**"
        .tapError:
          error =>
            ZIO.succeed:
              println:
                "Log: " + error

    defer:
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
                  "Log: Database Timeout"
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
    .onInterrupt(
      ZIO.debug("Log: Interrupting slow request")
    )
  end saveUser

  def sendToManualQueue(username: String) =
    ZIO
      .attempt("User sent to manual setup queue")

  val logUserSignup =
    ZIO.succeed(
      println(
        s"Log: Signup initiated for $userName"
      )
    )

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

```scala mdoc:silent
val effect0 =
  saveUser:
    userName
```


This `val` contains the logic of the Effect.
The Effect does not execute until we explicitly run it.

```scala mdoc
runScenario(
  scenario =
    HappyPath,
  logic =
    effect0
)
```

`runScenario(scenario = HappyPath)` runs our Effect in the "happy path" so that it will not fail.
This allows us to simulate failure scenarios in the next examples.

In real systems, assuming the "happy path" causes strange errors for users because the errors are unhandled.

We can also run `effect` in a scenario that will cause it to fail.

```scala mdoc
runScenario(
  scenario =
    DoesNotWorkInitially,
  logic =
    effect0
)
```

`runScenario(scenario = DoesNotWorkInitially)` runs our Effect but it fails.
The output logs the failure and the program produces the failure as the result of execution.

## Superpower: What if Failure is Temporary?

Sometimes things work when you keep trying.  
We can attach a `retry` to our first Effect.
`retry` accepts a `Schedule` that determines when to retry.

```scala mdoc:silent
import Schedule.{recurs, spaced}
val effect1 =
  effect0.retry:
    recurs(3) && spaced(1.second)
```

The Effect with the retry behavior becomes a new Effect and can optionally be assigned to a `val` (as is done here).
`recurs(3)` builds a `Schedule` that happens 3 times.
`spaced(1.second)` is a `Schedule` that happens once per second, forever.
By combining them, we get a `Schedule` that does something only 3 times and once per second.
Schedules can be applied to many different capabilities.
We do this because we assume the failure will likely be resolved within 3 seconds.

```scala mdoc
runScenario(
  scenario =
    DoesNotWorkInitially,
  logic =
    effect1
)
```

The output shows that running the Effect failed twice trying to save the user, then it succeeded.

### What If It Never Succeeds?

```scala mdoc
runScenario(
  scenario =
    NeverWorks,
  logic =
    effect1
)
```

In the `NeverWorks` scenarios, the Effect failed its initial attempt, and failed the subsequent three retries.
It eventually returns the DB error to the user.

## Superpower: Users Like Nice Error Messages

Let's handle the error and return something nicer:

```scala mdoc:silent
val effect2 =
  effect1.orElseFail:
    "ERROR: User could not be saved"
```

```scala mdoc
runScenario(
  scenario =
    NeverWorks,
  logic =
    effect2
)
```

**Any** fallible Effect can attach a variety of error handling capabilities.
`orElseFail` transforms any failure into a user-friendly form.
We added the capability without restructuring the original Effect.
This is just one way to handle errors.
ZIO provides many variations, which we will not cover exhaustively.

The `orElseFail` is combined with the prior Effect that has the retry,
  creating another new Effect that has both error handling capabilities.
Like `retry`, the `orElseFail` can be added to any fallible Effect.

Not only can capabilities be added to any Effect, Effects can be combined and modified, producing new Effects.

## Superpower: Things Can Take a Long Time

```scala mdoc:silent
val effect3 =
  effect2.timeoutFail("Save timed out"):
    5.seconds
```

If the effect does not complete within 5 seconds, it is canceled.
Cancellation will shut down the effect in a predictable way.
The Effect System supports predictable cancellation of Effects.
Like the other capabilities for error handling, timeouts can be added to any Effect.

```scala mdoc
runScenario(
  scenario =
    FirstIsSlow,
  logic =
    effect3
)
```

Running the new Effect in the `FirstIsSlow` scenario causes it to take longer than the 5 second timeout.

## Superpower: Fallback From Failure

In some cases there may be a fallback for a failed Effect.

```scala mdoc:silent
val effect4 =
  effect3.orElse:
    sendToManualQueue:
      userName
```

The `orElse` creates a new Effect with a fallback.
The `sendToManualQueue` simulates alternative fallback logic.

```scala mdoc
runScenario(
  scenario =
    NeverWorks,
  logic =
    effect4
)
```

We run the effect again in the `NeverWorks` scenario,
  causing it to execute the fallback Effect.

## Superpower: Add Some Logging

Effects can be run concurrently and as an example,
  we can at the same time as the user is being saved,
  send an event to another system.

```scala mdoc:silent
val effect5 =
  effect4.fireAndForget:
    logUserSignup
```

`fireAndForget` is a convenience method we defined (in hidden code) that makes it easy to run two effects in parallel and ignore any failures on the `logUserSignup` Effect.

```scala mdoc
runScenario(
  scenario =
    HappyPath,
  logic =
    effect5
)
```

We run the effect again in the `HappyPath` scenario to demonstrate running the Effects in parallel.

We can add all sorts of custom behavior to our Effect type,
  and then invoke them regardless of error and result types.

## Superpower: How Long Do Things Take?

For diagnostic information you can track timing:

```scala mdoc:silent
val effect6 =
  effect5.timed
```

```scala mdoc
runScenario(
  scenario =
    HappyPath,
  logic =
    effect6
)
```
We run the Effect in the "HappyPath" Scenario; now the timing information is packaged with the original output `String`.

## Superpower: Maybe We Don't Want To Run Anything

Now that we have added all of these superpowers to our process,
  our lead engineer lets us known that a certain user should be prevented from using our system.

```scala mdoc:silent
val effect7 =
  effect6.when(userName != "Morty")
```

```scala mdoc
runScenario(
  scenario =
    HappyPath,
  logic =
    effect7
)
```
We can add behavior to the end of our complex Effect,
  that prevents it from ever executing in the first place.

## Many More Superpowers

These examples have shown only a glimpse into the superpowers we can add to **any** Effect.
There are even more we will explore in the following chapters.
