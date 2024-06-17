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

```scala 3 mdoc:silent
val bad =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"

// [E029] Pattern Match Exhaustivity Warning:
//     case ex: NetworkException =>
//     ^
// match may not be exhaustive.
//
// It would fail on pattern case: _: GpsException
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


## Handling Thrown Exceptions

So far we've used Effects to encapsulate failures, but you may have legacy code or external libraries which throw `Exception`s instead.
In these situations there are ways to wrap the `Exception` throwing code and instead have the failures as part of the Effect.

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

Using `ZIO.attempt` we can catch the thrown `Exception`s and instead encapsulate the failure in the Effect.

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
