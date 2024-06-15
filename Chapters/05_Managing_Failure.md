# Managing Failure

Given that Effects encapsulate the unpredictable parts of a system,
they must have a way to express failure.

## Handling Failures

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

enum Scenario:
  case HappyPath,
    TooCold,
    NetworkFailure,
    GPSFailure

class GpsException          extends Exception("GPS Failure")
class NetworkException extends Exception("Network Failure")

val scenarioConfig
    : Config[Option[Scenario]] =
  Config.Optional[Scenario](
    Config.fail("no default scenario")
  )

class ErrorsStaticConfigProvider(
    scenario: Scenario
) extends ConfigProvider:
  override def load[A](config: Config[A])(
      implicit trace: Trace
  ): IO[Config.Error, A] =
    ZIO.succeed(Some(scenario).asInstanceOf[A])

var scenarioForNonZio: Option[Scenario] = None

def happyPath =
  scenarioForNonZio = Some(Scenario.HappyPath)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.HappyPath
    )
  )

def networkFailure =
  scenarioForNonZio = Some(Scenario.NetworkFailure)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.NetworkFailure
    )
  )

def gpsFailure =
  scenarioForNonZio = Some(Scenario.GPSFailure)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.GPSFailure
    )
  )

def weird =
  scenarioForNonZio = Some(Scenario.TooCold)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.TooCold
    )
  )

case class Temperature(degrees: Int)

val getTemperature: ZIO[
  Any,
  GpsException | NetworkException,
  Temperature
] =
  defer:
    val maybeScenario =
      ZIO.config(scenarioConfig).orDie.run

    maybeScenario match
      case Some(Scenario.GPSFailure) =>
        ZIO
          .fail:
            GpsException()
          .run
      case Some(Scenario.NetworkFailure) =>
        ZIO
          .fail:
            NetworkException()
          .run
      case Some(Scenario.TooCold) =>
        ZIO
           .succeed:
             Temperature(-20)
           .run
      case _ =>
         ZIO
           .succeed:
             Temperature(35)
           .run
```

Let's say we have an Effect `getTemperature` which can fail as it tries to make a network request.
First let's run it in the "happy path" assuming it won't fail:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = happyPath

def run =
  getTemperature
```

As expected, the program succeeds with the temperature.
But if we run it and simulate a network failure, the program will fail.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  getTemperature
```

Without any failure handling, the program will not continue past the failed Effect.
For example, if we try to print something after trying to get the temperature,
  an unhandled failure will not allow the program to continue.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  defer:
    getTemperature.run
    Console.printLine("only prints if getTemperature succeeds").run
```

We can add various forms of failure handling.
One is to "catch" the failure and transform it into another Effect which can also succeed or fail.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

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

This time the second Effect will run because we've transformed the `getTemperature` failure into a successful result.

With `catchAll` we must handle all the types of failures and the implementation of `getTemperature` can only fail with an Exception.

We may want to be more specific about how we handle different types of failures.
For example, let's try to catch the `NetworkException`:

{{ TODO: mdoc seems to have a bug and is not outputting the compiler warning }}

```scala 3 mdoc:warn
val bad =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
```

This produces a compiler warning because our `catchAll` does not actually catch all possible types of failure.

We should handle all the types of failures:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

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

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsFailure

def run =
  defer:
    val result =
      temperatureAppComplete.run
    Console.printLine(s"Didn't fail, despite: $result").run
```

Since the new `temperatureAppComplete` can no longer fail, we can no longer "catch" failures.
Trying to do so will result in a compile error:

```scala 3 mdoc:fail
temperatureAppComplete.catchAll:
  case ex: Exception =>
    ZIO.succeed:
      "This cannot happen"
```

The types of failures from `getTemperature` were both an `Exception`.
But failures can be any type.
For example, we can change the `Exceptions` into another type, like a `String`
  (which is not a good idea, but shows that failures are not constrained to only be `Exception`s)

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val getTemperatureBad =
  getTemperature.catchAll:
    case e: Exception =>
      ZIO.fail:
        e.getMessage
```

Now we can again use `catchAll` but can handle the `String` failure type:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsFailure

def run =
  getTemperatureBad.catchAll:
    case s: String =>
      Console.printLine(s)
```

There are many different ways to handle failures with Effects.
You've already seen some of the others, like `retry` and `orElse` in the **Superpowers** chapter.

With Effects, failures are aggregated across the chain of Effects, unless handled.
For instance, a new `localize` Effect might fail with a new type:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class LocalizeFailure(s: String)

def localize(temperature: Temperature) =
  if temperature.degrees > 0 then
    ZIO.succeed("Not too cold.")
  else
    ZIO.fail:
      LocalizeFailure("**Machine froze**")
```

We can now create a new Effect from `getTemperature` and `localize` that can fail with either an `Exception` or a `LocalizeFailure`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

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

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = weird

def run =
  getTemperatureLocal.catchAll:
    case e: Exception =>
      Console.printLine(e.getMessage)
    case LocalizeFailure(s: String) =>
      Console.printLine(s)
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

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*



// since this function isn't a ZIO, it has to get the scenario from a var which is set when the bootstrap is set
def getTemperatureOrThrow(): String =
  scenarioForNonZio match
    case Some(Scenario.GPSFailure) =>
      throw GpsException()
    case Some(Scenario.NetworkFailure) =>
      throw NetworkException()
    case _ =>
      "35 degrees"
```

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

```scala 3 mdoc
import zio.*
import zio.direct.*

def render(value: String) =
  s"Temperature: $value"
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

def temperatureApp(): String =
  render:
    getTemperatureOrThrow()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO.attempt:
    temperatureApp()
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
If we don't make any attempt to handle our problem, the whole program blows up and shows the gory details to the user.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    temperatureApp()
```

### Manual Error Discovery

Exceptions do not convey a contract in either direction.

If you have been burned in the past by functions that throw surprise exceptions
  , you might defensively catch `Exception`s all over your program.
For this program, it could look like:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

def temperatureCatchingApp(): String =
  try
    render:
      getTemperatureOrThrow()
  catch
    case ex: Exception =>
      "Failure"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    temperatureCatchingApp()
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

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

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    temperatureCatchingMoreApp()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsFailure

def run =
  ZIO.succeed:
    temperatureCatchingMoreApp()
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

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val getTemperatureWrapped =
  ZIO.attempt:
    getTemperatureOrThrow()
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val displayTemperatureZWrapped =
  getTemperatureWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsException =>
      ZIO.succeed:
        "GPS problem"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = happyPath

def run =
  displayTemperatureZWrapped
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  displayTemperatureZWrapped
```

This is decent, but does not provide the maximum possible guarantees.
Look at what happens if we forget to handle one of our failures.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsFailure

def run =
  getTemperatureWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
```

The compiler does not catch this bug, and instead fails at runtime.
Take extra care when interacting with legacy code
, since we cannot automatically recognize these situations at compile time.
We can provide a fallback case that will report anything we missed:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

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

This lets us avoid the most egregious gaps in functionality, but does not take full advantage of ZIO's type-safety.

Unsurprisingly, writing your code to utilize Effects from the beginning yields the best results.
