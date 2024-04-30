# Superpowers with Effects


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/03_Superpowers.md)




Effects enable us to progressively add capabilities to increase reliability and control the unpredictable aspects.
In this chapter you will see that once we've defined parts of a program in terms of Effects, we gain some superpowers.
The reason we call it "superpowers" is that the capabilities you will see can be attached to **any** Effect.
For recurring concerns in our program, we do not want to create a bespoke solution for each context.
To illustrate this we will show a few examples of common capabilities applied to Effects.
Let's start with the "happy path" where we save a user to a database
(an Effect)
and then gradually add superpowers.

To start with we save a user to a database:

```scala
val userName =
  "Morty"
```


```scala
val effect0 =
  saveUser:
    userName
```

The Effect does not execute until we explicitly run it.
Effects can be run as "main" programs, embedded in other programs, or in tests.
Normally to run an Effect with ZIO as a "main" program we do this:
```scala

object MyApp extends ZIOAppDefault:
  def run =
    effect0
```

In this book, to avoid the excess lines, we can shorten this to:
```scala
def run =
  effect0
// Result: Success(User saved)
```

By default, the `saveUser` Effect runs in the "happy path" so that it will not fail.

We can explicitly specify the way in which this Effect will run by overriding the `bootstrap` value: 
```scala
override val bootstrap =
  happyPath

def run =
  effect0
// Result: Success(User saved)
```

This allows us to simulate failure scenarios in the next examples.

In real systems, assuming the "happy path" causes strange errors for users because the errors are unhandled.

We can also run `effect` in a scenario that will cause it to fail.

```scala
override val bootstrap =
  neverWorks

def run =
  effect0
// Log: **Database crashed!!**
// Result: Failure(Fail(**Database crashed!!**,Stack trace for thread "zio-fiber-193":
// 	at repl.MdocSession.MdocApp.saveUser.fail(<input>:74)
// 	at repl.MdocSession.MdocApp.saveUser.fail(<input>:78)
// 	at repl.MdocSession.MdocApp.saveUser(<input>:105)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:63)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:64)
// 	at mdoctools.ToRun.runSync(MdocHelpers.scala:69)))
```

`runScenario(scenario = DoesNotWorkInitially)` runs our Effect but it fails.
The output logs the failure and the program produces the failure as the result of execution.

## Superpower: What if Failure is Temporary?

Sometimes things work when you keep trying.  
We can attach a `retry` to our first Effect.
`retry` accepts a `Schedule` that determines when to retry.

```scala
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

```scala
override val bootstrap =
  doesNotWorkInitially

def run =
  effect1
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Result: Success(User saved)
```

The output shows that running the Effect failed twice trying to save the user, then it succeeded.

### What If It Never Succeeds?

```scala
override val bootstrap =
  neverWorks

def run =
  effect1
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Result: Failure(Fail(**Database crashed!!**,Stack trace for thread "zio-fiber-1040":
// 	at repl.MdocSession.MdocApp.saveUser.fail(<input>:74)
// 	at repl.MdocSession.MdocApp.saveUser.fail(<input>:78)
// 	at repl.MdocSession.MdocApp.saveUser(<input>:105)
// 	at repl.MdocSession.MdocApp.effect1(<input>:193)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:63)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:64)
// 	at mdoctools.ToRun.runSync(MdocHelpers.scala:69)))
```

In the `NeverWorks` scenarios, the Effect failed its initial attempt, and failed the subsequent three retries.
It eventually returns the DB error to the user.

## Superpower: Users Like Nice Error Messages

Let's handle the error and return something nicer:

```scala
val effect2 =
  effect1.orElseFail:
    "ERROR: User could not be saved"
```

```scala
override val bootstrap =
  neverWorks

def run =
  effect2
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Result: Failure(Fail(ERROR: User could not be saved,Stack trace for thread "zio-fiber-1300":
// 	at repl.MdocSession.MdocApp.effect2(<input>:231)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:63)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:64)
// 	at mdoctools.ToRun.runSync(MdocHelpers.scala:69)))
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

```scala
val effect3 =
  effect2.timeoutFail("Save timed out"):
    5.seconds
```

If the effect does not complete within 5 seconds, it is canceled.
Cancellation will shut down the effect in a predictable way.
The Effect System supports predictable cancellation of Effects.
Like the other capabilities for error handling, timeouts can be added to any Effect.

```scala
override val bootstrap =
  firstIsSlow

def run =
  effect3
// Log: Interrupting slow request
// Result: Failure(Fail(Save timed out,Stack trace for thread "zio-fiber-1581":
// 	at repl.MdocSession.MdocApp.effect3(<input>:254)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:63)
// 	at mdoctools.ToRun.runSync.e(MdocHelpers.scala:64)
// 	at mdoctools.ToRun.runSync(MdocHelpers.scala:69)))
```

Running the new Effect in the `FirstIsSlow` scenario causes it to take longer than the 5 second timeout.

## Superpower: Fallback From Failure

In some cases there may be a fallback for a failed Effect.

```scala
val effect4 =
  effect3.orElse:
    sendToManualQueue:
      userName
```

The `orElse` creates a new Effect with a fallback.
The `sendToManualQueue` simulates alternative fallback logic.

```scala
override val bootstrap =
  neverWorks

def run =
  effect4
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Log: **Database crashed!!**
// Result: Success(User sent to manual setup queue)
```

We run the effect again in the `NeverWorks` scenario,
  causing it to execute the fallback Effect.

## Superpower: Add Some Logging

Effects can be run concurrently and as an example,
  we can at the same time as the user is being saved,
  send an event to another system.

```scala
val effect5 =
  effect4.fireAndForget:
    logUserSignup
```

`fireAndForget` is a convenience method we defined (in hidden code) that makes it easy to run two effects in parallel and ignore any failures on the `logUserSignup` Effect.

```scala
override val bootstrap =
  happyPath

def run =
  effect5
// Log: Signup initiated for Morty
// Result: Success(User sent to manual setup queue)
```

We run the effect again in the `HappyPath` scenario to demonstrate running the Effects in parallel.

We can add all sorts of custom behavior to our Effect type,
  and then invoke them regardless of error and result types.

## Superpower: How Long Do Things Take?

For diagnostic information you can track timing:

```scala
val effect6 =
  effect5.timed
```

```scala
override val bootstrap =
  happyPath

def run =
  effect6
// Result: Success((PT0.047501388S,User sent to manual setup queue))
```
We run the Effect in the "HappyPath" Scenario; now the timing information is packaged with the original output `String`.

## Superpower: Maybe We Don't Want To Run Anything

Now that we have added all of these superpowers to our process,
  our lead engineer lets us known that a certain user should be prevented from using our system.

```scala
val effect7 =
  effect6.when(userName != "Morty")
```

```scala
override val bootstrap =
  happyPath

def run =
  effect7
// Result: Success(None)
```
We can add behavior to the end of our complex Effect,
  that prevents it from ever executing in the first place.

## Many More Superpowers

These examples have shown only a glimpse into the superpowers we can add to **any** Effect.
There are even more we will explore in the following chapters.
