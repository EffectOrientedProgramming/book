# Errors

1. Why errors as values
1. Creating & Handling
   1. Flexible error types
1. Collection of fallible operations (`collectAllSuccesses`)

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

```scala mdoc:invisible
enum Scenario:
  case HappyPath,
    NetworkError,
    GPSError
//  case NumberOfSlowCall(ref: Ref[Int])
//  case WorksOnTry(attempts: Int, ref: Ref[Int])

import zio.Runtime.default.unsafe

val invocations: Ref[Scenario] =
  Unsafe.unsafe(
    (u: Unsafe) =>
      given Unsafe =
        u
      unsafe
        .run(
          Ref.make[Scenario](Scenario.HappyPath)
        )
        .getOrThrowFiberFailure()
  )
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
                  invocations.set(scenario).run
                  logic.run
                .provide(Scope.default)
              )
              .withConsole(OurConsole)
          )
          .getOrThrowFiberFailure()
      println(res)
  }

def getScenario() =
  Unsafe.unsafe{
    (u: Unsafe) =>
      given Unsafe =
        u
      unsafe
        .run(invocations.get)
        .getOrThrowFiberFailure()
  }
  
  
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

```scala mdoc:invisible
class GpsFail()     extends Exception
class NetworkException() extends Exception

// TODO Hide definition? Then we won't see the internals of the scenario stuff.
// This would also makes the exceptions more surprising
def getTemperature(): String =
  getScenario() match
    case Scenario.GPSError =>
      throw GpsFail()
    case Scenario.NetworkError =>
      throw NetworkException()
    case Scenario.HappyPath =>
      "35 degrees"
```

We have an existing `getTemperature` function that can fail in unspecified ways.

```scala mdoc
def render(value: String) =
  s"Temperature: $value"
```

```scala mdoc
def temperatureApp(): String =
  render:
    getTemperature()

runScenario(
  scenario =
    Scenario.HappyPath,
  ZIO.attempt:
    temperatureApp()
)
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
If we don't make any attempt to handle our problem, the whole program blows up and shows the gory details to the user.

```scala mdoc
runScenario(
  scenario =
    Scenario.NetworkError,
  ZIO.succeed:
    temperatureApp()
)
```

## Manual Error Discovery

If you have been burned in the past by functions that throw surprise exceptions
  , you might defensively catch `Exception`s all over your program.
For this program, it could look like:

```scala mdoc:nest
def temperatureApp(): String =
    try
      render:
        getTemperature()
    catch
      case ex: Exception =>
        "Failure"

runScenario(
  Scenario.NetworkError,
  ZIO.succeed:
    temperatureApp()
)
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```scala mdoc:nest
def temperatureApp(): String =
  try
    render:
      getTemperature()
  catch
    case ex: NetworkException =>
      "Network Unavailable"
    case ex: GpsFail =>
      "GPS Hardware Failure"

runScenario(
  Scenario.NetworkError,
  ZIO.succeed:
    temperatureApp()
)

runScenario(
  Scenario.GPSError,
  ZIO.succeed:
    temperatureApp()
)
```

Wonderful!
We have specific messages for all relevant error cases. However, this still suffers from downsides that become more painful as the codebase grows.

- We do not know if `temperatureApp` can fail
- Once we know it can fail, we must dig through the documentation or implementation to discover the different possibilities
- Because every function that is called by `temperatureApp` can call other functions, which can call other functions, and so on,
   we are never sure that we have found all the failure paths in our application

## More Problems with Exceptions

Exceptions have other problems:

1. The only way to ensure your program won't crash is by testing it through all possible execution paths. 

1. It is difficult or impossible to retry an operation if it fails.

Exceptions were a valiant attempt to produce a consistent error-reporting interface, and they are better than what came before.
You just don't know what you're going to get when you use exceptions.


## ZIO-First Error Handling

ZIO enables more powerful, uniform error-handling.

```scala mdoc:invisible
// TODO We hide the original implementation of this function, but show this one.
// Is that a problem? Seems unbalanced
val getTemperatureZ: ZIO[Any, GpsFail | NetworkException, String] =
   defer:
      print("") // Ungodly workaround to flush the output??? Mdoc grows more mysterious.
      getScenario() match
        case Scenario.GPSError =>
          ZIO.fail:
            GpsFail()
          .run
        case Scenario.NetworkError =>
          // TODO Use a non-exceptional error
          ZIO.fail:
            NetworkException()
          .run
        case Scenario.HappyPath =>
          ZIO.succeed:
            "Temperature: 35 degrees"
          .run

```

```scala mdoc
runScenario(Scenario.HappyPath, getTemperatureZ)
```

Running the ZIO version without handling any errors
```scala mdoc
runScenario(
  Scenario.NetworkError,
  getTemperatureZ
)
```

This is not an error that we want to show the user.
Instead, we want to handle all of our internal errors, and make sure that they result in a user-friendly error message.

```scala mdoc:fail
// TODO make MDoc:fail adhere to line limits?
runScenario(
  Scenario.NetworkError,
  getTemperatureZ.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
)
```

ZIO distinguishes itself here by alerting us that we have not caught all possible errors.
The compiler prevents us from executing non-exhaustive blocks inside of a `catchAll`.

```scala mdoc:silent
val temperatureAppZ =
  getTemperatureZ.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsFail =>
      ZIO.succeed:
        "GPS Hardware Failure"
```

```scala mdoc
runScenario(
  Scenario.GPSError,
  temperatureAppZ
)
```

Now that we have handled all of our errors, we know we are showing the user a sensible message.

Further, this is tracked by the compiler, which will prevent us from invoking `.catchAll` again.

```scala mdoc:fail
runScenario(
  Scenario.GPSError,
  temperatureAppZ.catchAll:
    case ex: Exception => 
      ZIO.succeed:
        "This cannot happen"
)
```

This will also prevent calls to other methods that depend on a non-`Nothing` error type.

- retry*
- orElse*
- mapError
- fold*
- merge
- refine*
- tapError*


Because of the type management provided by the effect library
, the compiler recognizes that this `retryN` can never be used and prevents us from calling it.

## Wrapping Legacy Code

If we are unable to re-write the fallible function, we can still wrap the call.

```scala mdoc:silent
val calculateTempWrapped =
  ZIO.attempt:
    getTemperature()
```

```scala mdoc:silent
val displayTemperatureZWrapped =
  calculateTempWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsFail =>
      ZIO.succeed:
        "GPS problem"
```

```scala mdoc
runScenario(
  Scenario.HappyPath,
  displayTemperatureZWrapped
)
```

```scala mdoc
runScenario(
  Scenario.NetworkError,
  displayTemperatureZWrapped
)
```

This is decent, but does not provide the maximum possible guarantees. 
Look at what happens if we forget to handle one of our errors.


```scala mdoc
runScenario(
  Scenario.GPSError,
  calculateTempWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
)
```

The compiler does not catch this bug, and instead fails at runtime.
Take extra care when interacting with legacy code
, since we cannot automatically recognize these situations at compile time.
We can provide a fallback case that will report anything we missed:

```scala mdoc
runScenario(
  Scenario.GPSError,
  calculateTempWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case other =>
      // TODO Decide if succeed is right
      ZIO.succeed:
        "Error: " + other
)
```

This lets us avoid the most egregious gaps in functionality, but it does not take full advantage of ZIO's type-safety.

> Note: The following is copy&pasted and needs work

