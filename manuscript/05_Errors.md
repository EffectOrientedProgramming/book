# Errors


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/05_Errors.md)


{{ potentially rename to Failure and then consistently use "Errors" or the "Failures" - side note: it is `ZIO.fail` }}

Given that Effects encapsulate the unpredictable parts of a system,
they must have a way to express errors.

Let's start with a basic Effect that has simulated unpredictability:

```scala
def canFail(succeeds: Boolean) =
  if succeeds then
    ZIO.succeed("it works")
  else
    ZIO.fail("*** FAIL ***")
```

Note that this function does not need to be an Effect because there are no unpredictable aspects
For this book we need a predictable way to show unpredictability
Generally - don't write code like this.

First, let's run the `canFail` Effect with an argument of `true` and print its result.

```scala
def run =
  canFail(succeeds =
    true
  ).debug
// it works
// Result: it works
```

Given our controlled behavior of the Effect, we see that the Effect succeeded.

If we now pass `false` to `canFail` the Effect will fail.

```scala
def run =
  canFail(succeeds =
    false
  ).debug("Things went wrong")
// <FAIL> Things went wrong: Fail(*** FAIL ***,Stack trace for thread "zio-fiber-577994243":
// 	at repl.MdocSession.MdocApp.canFail(<input>:11)
// 	at repl.MdocSession.MdocApp.Chapter23.run(<input>:34)
// 	at mdoctools.Rendering.renderEveryPossibleOutcomeZio(Rendering.scala:22)
// 	at mdoctools.Rendering.renderEveryPossibleOutcomeZio(Rendering.scala:32)
// 	at mdoctools.Rendering.renderEveryPossibleOutcomeZio(Rendering.scala:39)
// 	at mdoctools.Rendering.renderEveryPossibleOutcomeZio(Rendering.scala:46)
// Result: *** FAIL ***
```

Systems need to deal with failures and, ideally, recover from them.
We can apply a very basic recovery operation on the previous example called `flip` which swaps the error and the success values:

```scala
def run =
  canFail(succeeds =
    false
  ).flip
// Result: *** FAIL ***
```

Now the code succeeds because the `failure` is swapped into the success value.
Generally `flip` is for test functions because there aren't many use cases for overriding your success value with a failure.

There are other more useful operations to recover from failure including fallbacks, retries, and catchers.

{{ maybe basic retry example building on the previous? }}

1. Why errors as values
1. Creating & Handling
   1. Flexible error types

## Our program for this chapter

We want to show the user a page that shows the current temperature at their location
It will look like this

```text

Temperature: 30 degrees
```

There are 2 error situations we need to handle:

- Network call to weather service fails.
- A fault in our GPS hardware

We want our program to always result in a sensible message to the user.


## Throwing Exceptions

Throwing exceptions is one way indicate failure.

In a language that cannot `throw`, following the execution path is simple, following 2 basic rules:

- At a branch, execute first match
- Otherwise, Read everything:
  - left-to-right
  - top-to-bottom,

Once you add `throw`, the world gets more complicated.

- Unless we `throw`, jumping through a different dimension

TODO Prose transition here

We have an existing `getTemperatureOrThrow` function that can fail in unspecified ways.

```scala
def render(value: String) =
  s"Temperature: $value"
```

```scala
def temperatureApp(): String =
  render:
    getTemperatureOrThrow()
```

```scala
def run =
  ZIO.attempt:
    temperatureApp()
// Result: Temperature: 35 degrees
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
If we don't make any attempt to handle our problem, the whole program blows up and shows the gory details to the user.

```scala
scenario =
  ErrorsScenario.NetworkError

def run =
  ZIO.succeed:
    temperatureApp()
// Result: Defect: NetworkException
```

### Manual Error Discovery

Exceptions do not convey a contract in either direction.

If you have been burned in the past by functions that throw surprise exceptions
  , you might defensively catch `Exception`s all over your program.
For this program, it could look like:

```scala
def temperatureCatchingApp(): String =
  try
    render:
      getTemperatureOrThrow()
  catch
    case ex: Exception =>
      "Failure"
```

```scala
scenario =
  ErrorsScenario.NetworkError

def run =
  ZIO.succeed:
    temperatureCatchingApp()
// Result: Failure
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```scala
def temperatureCatchingMoreApp(): String =
  try
    render:
      getTemperatureOrThrow()
  catch
    case ex: NetworkException =>
      "Network Unavailable"
    case ex: GpsFail =>
      "GPS Hardware Failure"
```

```scala
scenario =
  ErrorsScenario.NetworkError

def run =
  ZIO.succeed:
    temperatureCatchingMoreApp()
// Result: Network Unavailable
```

```scala
scenario =
  ErrorsScenario.GPSError

def run =
  ZIO.succeed:
    temperatureCatchingMoreApp()
// Result: GPS Hardware Failure
```

Wonderful!
We have specific messages for all relevant error cases. However, this still suffers from downsides that become more painful as the codebase grows.

- We do not know if `temperatureApp` can fail
- Once we know it can fail, we must dig through the documentation or implementation to discover the different possibilities
- Because every function that is called by `temperatureApp` can call other functions, which can call other functions, and so on,
   we are never sure that we have found all the failure paths in our application

### More Problems

Exceptions have other problems:

1. The only way to ensure your program won't crash is by testing it through all possible execution paths.

1. It is difficult or impossible to retry an operation if it fails.

Exceptions were a valiant attempt to produce a consistent error-reporting interface, and they are better than what came before.
You just don't know what you're going to get when you use exceptions.

## Error Handling with ZIO

ZIO enables more powerful, uniform error-handling.


```scala
override val bootstrap =
  errorsHappyPath

def run =
  getTemperature
// Result: Temperature: 35 degrees
```

Running the ZIO version without handling any errors

```scala
override val bootstrap =
  errorsNetworkError

def run =
  getTemperature
// Result: repl.MdocSession$MdocApp$NetworkException
```

This is not an error that we want to show the user.
Instead, we want to handle all of our internal errors, and make sure that they result in a user-friendly error message.

{{ TODO: mdoc seems to have a bug and is not outputting the compiler warning }}

```scala
val bad =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
//
```

ZIO distinguishes itself here by alerting us that we have not caught all possible errors.
The compiler prevents us from executing non-exhaustive blocks inside of a `catchAll`.

```scala
val temperatureAppComplete =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsFail =>
      ZIO.succeed:
        "GPS Hardware Failure"
```

```scala
override val bootstrap =
  errorsGpsError

def run =
  temperatureAppComplete
// Result: GPS Hardware Failure
```

Now that we have handled all of our errors, we know we are showing the user a sensible message.

Further, this is tracked by the compiler, which will prevent us from invoking `.catchAll` again.

```scala
temperatureAppComplete.catchAll:
  case ex: Exception =>
    ZIO.succeed:
      "This cannot happen"
// error: 
// This error handling operation assumes your effect can fail. However, your effect has Nothing for the error type, which means it cannot fail, so there is no need to handle the failure. To find out which method you can use instead of this operation, please see the reference chart at: https://zio.dev/can_fail.
// I found:
// 
//     CanFail.canFail[E](/* missing */summon[scala.util.NotGiven[E =:= Nothing]])
// 
// But no implicit values were found that match type scala.util.NotGiven[E =:= Nothing].
```

The compiler also ensures that we only call the following methods on effects that can fail:

- retry*
- orElse*
- mapError
- fold*
- merge
- refine*
- tapError*

## Wrapping Legacy Code

If we are unable to re-write the fallible function, we can still wrap the call.

TODO Explain ZIO.attempt

```scala
val getTemperatureWrapped =
  ZIO.attempt:
    getTemperatureOrThrow()
```

```scala
val displayTemperatureZWrapped =
  getTemperatureWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsFail =>
      ZIO.succeed:
        "GPS problem"
```

```scala
scenario =
  ErrorsScenario.HappyPath

def run =
  displayTemperatureZWrapped
// Result: 35 degrees
```

```scala
scenario =
  ErrorsScenario.NetworkError

def run =
  displayTemperatureZWrapped
// Result: Network Unavailable
```

This is decent, but does not provide the maximum possible guarantees.
Look at what happens if we forget to handle one of our errors.

```scala
scenario =
  ErrorsScenario.GPSError

def run =
  getTemperatureWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
// Result: Defect: GpsFail
```

The compiler does not catch this bug, and instead fails at runtime.
Take extra care when interacting with legacy code
, since we cannot automatically recognize these situations at compile time.
We can provide a fallback case that will report anything we missed:

```scala
scenario =
  ErrorsScenario.GPSError

def run =
  getTemperatureWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case other =>
      ZIO.succeed:
        "Unknown Error"
// Result: Unknown Error
```

This lets us avoid the most egregious gaps in functionality, but does not take full advantage of ZIO's type-safety.

## ZIO super powers for errors

### Prevents Overly-Defensive Programming

Anything can be wrapped with a try/catch.
Things that produce exceptions don't need to be wrapped with trys.

You are forbidden from `retry`'ing effects that cannot fail.

```scala
ZIO
  .succeed(println("Always gonna work"))
  .retryN(100)
// error:
// This error handling operation assumes your effect can fail. However, your effect has Nothing for the error type, which means it cannot fail, so there is no need to handle the failure. To find out which method you can use instead of this operation, please see the reference chart at: https://zio.dev/can_fail.
// I found:
// 
//     CanFail.canFail[E](/* missing */summon[scala.util.NotGiven[E =:= Nothing]])
// 
// But no implicit values were found that match type scala.util.NotGiven[E =:= Nothing].
// ZIO
//    ^
```

```scala
ZIO
  .attempt(println("This might work"))
  .retryN(100)
```

### Flexible error types

### Collections of Error

eg `collectAllSuccesses`
