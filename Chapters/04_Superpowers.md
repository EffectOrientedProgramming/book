# Superpowers with Effects

Effects enable us to progressively add capabilities to a program to increase its reliability and control the unpredictable aspects.
In this chapter you will see that once we've defined parts of a program in terms of effects, we gain some superpowers.
The reason we call it "superpowers" is that the capabilities you will see can be attached to **any** effect. For recurring concerns in our program, we do not want to create a bespoke solution for each context.
To illustrate this we will show a few examples of common capabilities applied to effects.
Let's start with the "happy path" where we save a user to a database (an effect) and then gradually add superpowers.

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
```

## Step 1. The Happy Path is The Wrong Path

To start with we save a user to a database:

```scala mdoc:silent
val userName =
  "Morty"
```

```scala mdoc:silent
val step1 =
  saveUser:
    userName
```

```scala mdoc
runDemo:
  step1
```

```scala mdoc:invisible
HiddenPrelude.resetScenario(Scenario.NeverWorks)
```

In a real system this gives our users strange errors because they are unhandled.

## Step 2. What if Failure is Temporary?

```scala mdoc
HiddenPrelude
  .resetScenario(Scenario.doesNotWorkInitially)
```

Sometimes things work when you keep trying.  We can use a schedule to determine how to keep trying:

TODO {{runDemo should use a super fast clock so our builds aren't slow}}
```scala mdoc:silent
val aFewTimes =
  Schedule.spaced(1.milli) && Schedule.recurs(3)
```

`spaced(1.second)` is a `Schedule` that happens once per second, forever.
`recurs(3)` builds a `Schedule` that happens 3 times.
By combining them, we get a `Schedule` that does something once per second, but only 3 times.
Schedules can be applied to many different capabilities.
We can add lines to the previous example to apply a `retry` to the effect.
We do this because we assume the failure will likely be resolved within 3 seconds:

```scala mdoc:silent
val step2 =
  step1.retry:
    aFewTimes
```

```scala mdoc
runDemo:
  step2
```

You can see from the output that we failed twice trying to save the user, then it succeeded.

### What if it never succeeds?

```scala mdoc
HiddenPrelude.resetScenario(Scenario.NeverWorks)
```

This uber-super power is further illustrated when the retries do not ultimately succeed:

```scala mdoc
runDemo:
  step2
```

In this run of the program, the effect failed its initial attempt, and failed the subsequent three retries.  The final failure was handled by the `orElseFail`.


## Step 3. Users like nice error messages

Let's handle the error and return something nicer:

```scala mdoc:silent
val step3 =
  step2.orElseFail:
    "ERROR: User could not be saved"
```

```scala mdoc
runDemo:
  step3
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

## Step 4. Timeouts

```scala mdoc
HiddenPrelude.resetScenario(Scenario.firstIsSlow)
```
```scala mdoc:silent
val step4 =
  step3.timeoutFail("Took too long to save"):
    // TODO Restore real value when done editing
    5.millis
//      5.seconds
```

```scala mdoc
runDemo:
  step4
```

## Step 5. Fallback Effect

```scala mdoc
HiddenPrelude.resetScenario(Scenario.NeverWorks)
```

```scala mdoc:silent
val step5 =
  step4.orElse:
    sendToManualQueue:
      userName
```
```scala mdoc
// fails - with retry and fallback
runDemo:
  step5
```

## Step 6. Concurrently Execute Effect 
TODO Consider deleting. Uses an extension

```scala mdoc
HiddenPrelude
  .resetScenario(Scenario.WorksFirstTime)
```

```scala mdoc:silent
val step6 =
  step5
    // todo: maybe this hidden extension method
    // goes too far with functionality that
    // doesn't exist in vanilla ZIO
    .fireAndForget:
      userSignupInitiated:
        userName
```

`fireAndForget` is an extension method, whose implementation we are hiding for now.
It executes the new effect in parallel, so even though we have added it "to the end" of our larger existing effect,
  it completes first.

```scala mdoc
runDemo:
  step6
```

## Step 7. Timing all of this

```scala mdoc:silent
val step7 =
  step6.timed
```

```scala mdoc
HiddenPrelude
  .resetScenario(Scenario.WorksFirstTime)
```

```scala mdoc
runDemo:
  step7
```

## Step 8. Maybe we don't want this to run at all?

Prose about wanting to lock Morty out?

```scala mdoc:silent
val step8 =
  step7.when(userName != "Morty")
```

```scala mdoc
HiddenPrelude
  .resetScenario(Scenario.WorksFirstTime)
```

```scala mdoc
runDemo:
  step8
```

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
