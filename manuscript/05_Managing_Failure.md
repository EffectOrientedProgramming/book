# Managing Failure


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/05_Managing_Failure.md)


Given that Effects encapsulate the unpredictable parts of a system,
they must have a way to express failure.

## Handling Failures


Let's say we have an Effect `getTemperature` which can fail as it tries to make a network request.
First let's run it in the "happy path" assuming it won't fail:

```scala
override val bootstrap = happyPath

def run =
  getTemperature
```

Output:

```shell
Result: Temperature: 35 degrees
```

As expected, the program succeeds with the temperature.
But if we run it and simulate a network failure, the program will fail.

```scala
override val bootstrap = networkFailure

def run =
  getTemperature
```

Output:

```shell
Result: Defect: NetworkException: Network Failure
```

Without any failure handling, the program will not continue past the failed Effect.
For example, if we try to print something after trying to get the temperature,
  an unhandled failure will not allow the program to continue.

```scala
override val bootstrap = networkFailure

def run =
  defer:
    getTemperature.run
    Console.printLine("will not print if getTemperature fails").run
```

Output:

```shell
Result: Defect: NetworkException: Network Failure
```

We can add various forms of failure handling.
One is to "catch" the failure and transform it into another Effect which can also succeed or fail.

```scala
override val bootstrap = networkFailure

def run =
  val safeGetTemperature =
    getTemperature.catchAll:
      case e: Exception =>
        ZIO.succeed("Could not get temperature")

  defer:
    safeGetTemperature.run
    Console.printLine("will not print if getTemperature fails").run
```

Output:

```shell
will not print if getTemperature fails
```

This time the second Effect will run because we've transformed the `getTemperature` failure into a successful result.

With `catchAll` we must handle all the types of failures and the implementation of `getTemperature` can only fail with an Exception.

We may want to be more specific about how we handle different types of failures.
For example, let's try to catch the `NetworkException`:


```scala
val bad =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
```

Output:

```shell

```

This produces a compiler warning because our `catchAll` does not actually catch all possible types of failure.

We should handle all the types of failures:

```scala
val temperatureAppComplete =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsException =>
      ZIO.succeed:
        "GPS Hardware Failure"
```

Now even if there is a network or GPS failure, the Effect completes successfully:

```scala
override val bootstrap = gpsFailure

def run =
  defer:
    val result =
      temperatureAppComplete.run
    Console.printLine(s"Didn't fail, despite: $result").run
```

Output:

```shell
Didn't fail, despite: GPS Hardware Failure
```

Since the new `temperatureAppComplete` can no longer fail, we can no longer "catch" failures.
Trying to do so will result in a compile error:

{{ TODO: better compiler error message? }}

```scala
temperatureAppComplete.catchAll:
  case ex: Exception =>
    ZIO.succeed:
      "This cannot happen"
```

Output:

```shell
error: 
This error handling operation assumes your effect can fail. However, your effect has Nothing for the error type, which means it cannot fail, so there is no need to handle the failure. To find out which method you can use instead of this operation, please see the reference chart at: https://zio.dev/can_fail.
I found:

    CanFail.canFail[E](/* missing */summon[scala.util.NotGiven[E =:= Nothing]])

But no implicit values were found that match type scala.util.NotGiven[E =:= Nothing].
```

The types of failures from `getTemperature` were both an `Exception`.
But failures can be any type.
For example, we can change the `Exceptions` into another type, like a `String`
  (which is not a good idea, but shows that failures are not constrained to only be `Exception`s)

```scala
val getTemperatureBad =
  getTemperature.catchAll:
    case e: Exception =>
      ZIO.fail:
        e.getMessage
```

Now we can again use `catchAll` but can handle the `String` failure type:

```scala
override val bootstrap = gpsFailure

def run =
  getTemperatureBad.catchAll:
    case s: String =>
      Console.printLine(s)
```

Output:

```shell
GPS Failure
```

There are many different ways to handle failures with Effects.
You've already seen some of the others, like `retry` and `orElse` in the **Superpowers** chapter.

With Effects, failures are aggregated across the chain of Effects, unless handled.
For instance, a new `localize` Effect might fail with a new type:

```scala
case class LocalizeFailure(s: String)

def localize(temperature: String) =
  if temperature.contains("35") then
    ZIO.succeed("Brrrr")
  else
    ZIO.fail:
      LocalizeFailure("I dunno")
```

We can now create a new Effect from `getTemperature` and `localize` that can fail with either an `Exception` or a `LocalizeFailure`:

```scala
// can fail with an Exception or a LocalizeFailure
val getTemperatureLocal =
  defer:
    // can fail with an Exception
    val temperature =
      getTemperature.run

    // can fail with a LocalizeFailure
    localize(temperature).run
```

To handle the possible failures for this new Effect, we now need to handle both the `Exception` and `LocalizeFailure`:

```scala
override val bootstrap = weird

def run =
  getTemperatureLocal.catchAll:
    case e: Exception =>
      Console.printLine(e.getMessage)
    case LocalizeFailure(s: String) =>
      Console.printLine(s)
```

Output:

```shell
I dunno
```

All the possible failure types, across the sequence of Effects, have been handled at the top-level Effect.
It is up to you when and how you want to handle possible failures.


## Wrapping Exceptions

{{ TODO: ZIO.attempt }}


{{ TODO: Old structure below, time-permitting we refactor, or just leave this all as-is }}

## Our program for this chapter

We want to show the user a page that shows the current temperature at their location
It will look like this

```text
Temperature: 30 degrees
```

There are 2 failure situations we need to handle:

- Network call to weather service fails.
- A fault in our GPS hardware

We want our program to always result in a sensible message to the user.

{{ TODO: maybe show Exception classes so that we can later show non-Exception failures }}


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
```

Output:

```shell
Result: Temperature: 35 degrees
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
If we don't make any attempt to handle our problem, the whole program blows up and shows the gory details to the user.

```scala
override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    temperatureApp()
```

Output:

```shell
Result: Defect: NetworkException: Network Failure
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
override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    temperatureCatchingApp()
```

Output:

```shell
Result: Failure
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
    case ex: GpsException =>
      "GPS Hardware Failure"
```

```scala
override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    temperatureCatchingMoreApp()
```

Output:

```shell
Result: Network Unavailable
```

```scala
override val bootstrap = gpsFailure

def run =
  ZIO.succeed:
    temperatureCatchingMoreApp()
```

Output:

```shell
Result: GPS Hardware Failure
```

Wonderful!
We have specific messages for all relevant failure cases. However, this still suffers from downsides that become more painful as the codebase grows.

- We do not know if `temperatureApp` can fail
- Once we know it can fail, we must dig through the documentation or implementation to discover the different possibilities
- Because every function that is called by `temperatureApp` can call other functions, which can call other functions, and so on,
   we are never sure that we have found all the failure paths in our application
- It is difficult or impossible to retry an operation if it fails.

Exceptions were a valiant attempt to produce a consistent failure-reporting interface, and they are better than what came before.
You just don't know what you're going to get when you use exceptions.

## Error Handling with ZIO

ZIO enables more powerful, uniform failure-handling.




This is not a failure that we want to show the user.
Instead, we want to handle all of our internal failure, and make sure that they result in a user-friendly failure message.



ZIO distinguishes itself here by alerting us that we have not caught all possible failures.
The compiler prevents us from executing non-exhaustive blocks inside a `catchAll`.



Now that we have handled all of our failures, we know we are showing the user a sensible message.

Further, this is tracked by the compiler, which will prevent us from invoking `.catchAll` again.



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
    case ex: GpsException =>
      ZIO.succeed:
        "GPS problem"
```

```scala
override val bootstrap = happyPath

def run =
  displayTemperatureZWrapped
```

Output:

```shell
Result: 35 degrees
```

```scala
override val bootstrap = networkFailure

def run =
  displayTemperatureZWrapped
```

Output:

```shell
Result: Network Unavailable
```

This is decent, but does not provide the maximum possible guarantees.
Look at what happens if we forget to handle one of our failures.

```scala
override val bootstrap = gpsFailure

def run =
  getTemperatureWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
```

Output:

```shell
Result: Defect: GpsException
```

The compiler does not catch this bug, and instead fails at runtime.
Take extra care when interacting with legacy code
, since we cannot automatically recognize these situations at compile time.
We can provide a fallback case that will report anything we missed:

```scala
override val bootstrap = gpsFailure

def run =
  getTemperatureWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case other =>
      ZIO.succeed:
        "Unknown Error"
```

Output:

```shell
Result: Unknown Error
```

This lets us avoid the most egregious gaps in functionality, but does not take full advantage of ZIO's type-safety.

Unsurprisingly, writing your code to utilize Effects from the beginning yields the best results.
