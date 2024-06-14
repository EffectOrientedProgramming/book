# Managing Failure

Given that Effects encapsulate the unpredictable parts of a system,
they must have a way to express failure.

{{ TODO: Refactor existing content into these }}

{{ TODO: GPS Example with 2 scenarios: success & failure }}

## Handling Failures

Catching, recovering, etc


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

enum Scenario:
  case HappyPath,
    NetworkError,
    GPSError

class GpsFail          extends Exception
class NetworkException extends Exception

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

def networkError =
  scenarioForNonZio = Some(Scenario.NetworkError)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.NetworkError
    )
  )

def gpsError =
  scenarioForNonZio = Some(Scenario.GPSError)

  Runtime.setConfigProvider(
    ErrorsStaticConfigProvider(
      Scenario.GPSError
    )
  )

// since this function isn't a ZIO, it has to get the scenario from a var which is set when the bootstrap is set
def getTemperatureOrThrow(): String =
  scenarioForNonZio match
    case Some(Scenario.GPSError) =>
      throw GpsFail()
    case Some(Scenario.NetworkError) =>
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

override val bootstrap = networkError

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

override val bootstrap = networkError

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
    case ex: GpsFail =>
      "GPS Hardware Failure"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkError

def run =
  ZIO.succeed:
    temperatureCatchingMoreApp()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsError

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

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

// TODO We hide the original implementation of this function, but show this one.
// Is that a problem? Seems unbalanced
val getTemperature: ZIO[
  Any,
  GpsFail | NetworkException,
  String
] =
  defer:
    val maybeScenario =
      ZIO.config(scenarioConfig).orDie.run
      
    maybeScenario match
      case Some(Scenario.GPSError) =>
        ZIO
          .fail:
            GpsFail()
          .run
      case Some(Scenario.NetworkError) =>
        ZIO
          .fail:
            NetworkException()
          .run
      case _ =>
         ZIO
           .succeed:
             "Temperature: 35 degrees"
           .run
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = happyPath

def run =
  getTemperature
```

Running the ZIO version without handling any failure

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkError

def run =
  getTemperature
```

This is not a failure that we want to show the user.
Instead, we want to handle all of our internal failure, and make sure that they result in a user-friendly failure message.

{{ TODO: mdoc seems to have a bug and is not outputting the compiler warning }}

```scala 3 mdoc:warn
val bad =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
```

ZIO distinguishes itself here by alerting us that we have not caught all possible failures.
The compiler prevents us from executing non-exhaustive blocks inside a `catchAll`.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val temperatureAppComplete =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsFail =>
      ZIO.succeed:
        "GPS Hardware Failure"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsError

def run =
  temperatureAppComplete
```

Now that we have handled all of our failures, we know we are showing the user a sensible message.

Further, this is tracked by the compiler, which will prevent us from invoking `.catchAll` again.

```scala 3 mdoc:fail
temperatureAppComplete.catchAll:
  case ex: Exception =>
    ZIO.succeed:
      "This cannot happen"
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
    case ex: GpsFail =>
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

override val bootstrap = networkError

def run =
  displayTemperatureZWrapped
```

This is decent, but does not provide the maximum possible guarantees.
Look at what happens if we forget to handle one of our failures.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsError

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

override val bootstrap = gpsError

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
