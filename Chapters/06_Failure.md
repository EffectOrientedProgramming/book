# Failure

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

class GpsException
    extends Exception("GPS Failure")
class NetworkException
    extends Exception("Network Failure")

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
    ZIO.succeed(
      Some(scenario).asInstanceOf[A]
    )

var scenarioForNonZio: Option[Scenario] =
  None

def happyPath =
  scenarioForNonZio =
    Some(Scenario.HappyPath)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.HappyPath
    )
  )

def networkFailure =
  scenarioForNonZio =
    Some(Scenario.NetworkFailure)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.NetworkFailure
    )
  )

def gpsFailure =
  scenarioForNonZio =
    Some(Scenario.GPSFailure)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.GPSFailure
    )
  )

def tooCold =
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
  Temperature,
] =
  defer:
    val maybeScenario =
      ZIO.config(scenarioConfig).orDie.run
    printLine("Getting Temperature").orDie.run

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
    end match
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

// TODO Reduce output here

def run =
  getTemperature
```

Unless we handle failure, the program ends here.
For example, if we try to print something after trying to get the temperature,
  an unhandled failure will not allow the program to continue.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

override val bootstrap = networkFailure

def run =
  defer:
    getTemperature.run
    printLine("getTemperature succeeded").run
```

We handle failure in various ways.
One is to "catch" the failure and transform it into another Effect which can also succeed or fail.
Note that this is a method in our Effect System, *not* the `catch` mechanism of the language.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

override val bootstrap = networkFailure

def run =
  val safeGetTemperature =
    getTemperature.catchAll:
      case e: Exception =>
        ZIO.succeed:
          "Could not get temperature"

  defer:
    val result = safeGetTemperature.run
    printLine(result).run
```

This time the second Effect will run because we've transformed the `getTemperature` failure into a successful result.

With `catchAll` we must handle all the types of failures and the implementation of `getTemperature` can only fail with an Exception.

We may want to be more specific about how we handle different types of failures.
For example, let's try to catch the `NetworkException`:

<!-- We do not use mdoc:warn because of bugs in mdoc -->

```scala 3 mdoc:silent
val bad =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
// Pattern Match Exhaustivity Warning:
//     case ex: NetworkException =>
//     ^
// match may not be exhaustive.
//
// It would fail on pattern case: _: GpsException
```

This produces a compiler warning because our `catchAll` does not actually catch all possible failures.

We should handle all failures:

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
import zio.Console.*

override val bootstrap = gpsFailure

def run =
  defer:
    val result = temperatureAppComplete.run
    printLine(result).run
```

Since the new `temperatureAppComplete` can no longer fail, we can no longer "catch" failures.
Trying to do so will result in a compiler error:

```scala 3 mdoc:fail
temperatureAppComplete.catchAll:
  case ex: GpsException =>
    ZIO.succeed:
      "This cannot happen"
```

## Custom Error Types

The failures from `getTemperature` were both `Exception`s, but failures can be any type.

Consider a `check` Effect (implementation hidden) that can fail with a custom type:

```scala 3 mdoc
case class ClimateFailure(message: String)
```

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

def check(t: Temperature) =
  defer:
    printLine("Checking Temperature").run
    if t.degrees > 0 then
      ZIO.succeed:
        "Comfortable Temperature"
      .run
    else
      ZIO.fail:
        ClimateFailure("**Too Cold**")
      .run
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

def run =
  check(Temperature(-20))
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

def run =
  check(Temperature(15))
```

## Multiple Error Types 

```scala 3
// TODO Subheader name
```

We can now create a new Effect that calls `getTemperature` and then `check`.
The Effect System tracks all possible failures in a sequence of Effects.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val weatherReportFaulty =
  defer:
    val result = getTemperature.run
    check(result).run
```

To handle all possible failures for this new Effect, we must handle both `Exception` and `ClimateFailure`:

```scala 3 mdoc:silent
val weatherReport =
  weatherReportFaulty.catchAll:
    case exception: Exception =>
      printLine:
        exception.getMessage
    case failure: ClimateFailure =>
      printLine:
        failure.message
```

All possible failure types, across the sequence of Effects, are handled.
As before, the Effect System will produce a compiler error if we miss an error type.

When our combined Effect

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

override val bootstrap = tooCold

def run =
  weatherReport
```

## Short-circuiting Failures

Now we see what happens when a `gpsFailure` causes the *first* Effect to fail:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsFailure

def run =
  weatherReport
```

The program fails with the GPS failure, and the `check` Effect is not run.


Short-circuiting is an essential part of a user-friendly Effect Systems.
With it, we can write a linear sequence of fallible expressions, while tracking all possible failures.


## Handling Thrown Exceptions

So far our example Effects have **returned** `Exception`s to indicate failure, but you may have legacy code or external libraries which **throw** `Exception`s instead.
In these situations there are ways to wrap the `Exception`-throwing code so that we get back to our preferred style of returning `Exception`s.

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

We have an existing `getTemperatureOrThrow` function that can fail by throwing an `Exception`.

If we try to just call this function from an Effect, in the case of failure, the program will fail.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    getTemperatureOrThrow()
```
So despite our claim that this Effect would `succeed`, it actually crashes with a defect.
When we call side-effecting code, our Effect System can't warn us about the potential failure.

The right way is to use `ZIO.attempt` which captures thrown `Exception`s as the error type of the Effect.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val safeTemperatureApp =
  ZIO.attempt:
    getTemperatureOrThrow()
```

Then you can use any of the failure handling mechanisms in Effects to deal with the failure.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  safeTemperatureApp.orElse:
    ZIO.succeed:
      "Could not get temperature"
```

In this case we handle the Effect's failure with a fallback Effect which succeeds.

Since thrown `Exception`s are inherently unpredictable, it is preferable to encapsulate all functions that may throw, into Effects.
This helps make the unpredictability more clear and provides many mechanisms for handling the possible failures. 
