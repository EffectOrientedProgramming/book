# Errors

1. Why errors as values
1. Creating & Handling
   1. Flexible error types
1. Exhaustive checking DONE
   1. Covering all possibles (`catchAll` not missing any) DONE
   1. Do not handle impossible errors (`retry` not working if there is no error) DONE
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
      println("Result: " + res)
  }

def getScenario() =
  Unsafe.unsafe(
    (u: Unsafe) =>
      given Unsafe =
        u
      unsafe
        .run(invocations.get)
        .getOrThrowFiberFailure()
  )
  
  
```

## Historic approaches to Error-handling
If you are not interested in the discouraged ways to handle errors, and just want to see the ZIO approach, jump down to
[ZIO Error Handling](#zio-error-handling)

## Throwing Exceptions

Throwing exceptions is one way indicate failure.

In a language that cannot `throw`, following the execution path is simple, following 2 basic rules:

- At a branch, execute first match
- Otherwise, Read everything:
  - left-to-right
  - top-to-bottom,

Once you add `throw`, the rules are more complicated

- At a branch, execute first match
- Otherwise, Read everything
  - left-to-right
  - top-to-bottom
- Unless we `throw`, jumping through a different dimension



```scala mdoc:invisible

// TODO Hide definition? Then we won't see the internals of the scenario stuff.
// This would also makes the exceptions more surprising
def calculateTemp(): String =
  getScenario() match
    case Scenario.GPSError =>
      throw GpsException()
    case Scenario.NetworkError =>
      throw NetworkException()
    case Scenario.HappyPath =>
      "35 degrees"
```

```scala mdoc
class GpsException()     extends Exception
class NetworkException() extends Exception

def render(value: String) =
  s"Temperature: $value"
```

```scala mdoc
def currentTemperatureUnsafe(): String =
  render:
    calculateTemp()

runScenario(
  scenario =
    Scenario.HappyPath,
  ZIO.succeed:
    currentTemperatureUnsafe()
)
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
This can take many forms.
If we don't make any attempt to handle our problem, the whole program blows up and shows the gory details to the user.

```scala mdoc
// Note - Can't make this output prettier/simpler because it's *not* using ZIO
// Actually, now that we're using ZIO, we can make this output prettier.
// But maybe we should use a different runScenario method that _doesn't_ use ZIO?

runScenario(
  scenario =
    Scenario.NetworkError,
  ZIO.succeed:
    currentTemperatureUnsafe()
)
```

## Diligent Catching, without any hints.
We can take a more honest and accurate approach in this situation.

```scala mdoc:nest
def currentTemperature(): String =
  render:
    try
      calculateTemp()
    catch
      case ex: Exception =>
        "Unavailable"

runScenario(
  Scenario.NetworkError,
  ZIO.succeed:
    currentTemperature()
)
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```scala mdoc:nest
def currentTemperature(): String =
  try
    render:
      calculateTemp()
  catch
    case ex: NetworkException =>
      "Network Unavailable"
    case ex: GpsException =>
      "GPS problem"

runScenario(
  Scenario.NetworkError,
  ZIO.succeed:
    currentTemperature()
)

runScenario(
  Scenario.GPSError,
  ZIO.succeed:
    currentTemperature()
)
```

Wonderful!
We have specific messages for all relevant error cases. However, this still suffers from downsides that become more painful as the codebase grows.

- The signature of `currentTemperature` does not alert us that it might fail
- If we realize it can fail, we must dig through the implementation to discover the multiple failure values
- We never have certainty about the failure paths of our full application, or any subset of it.

## More Problems with Exceptions

Many languages use *exceptions* for handling errors.
An exception *throws* out of the current execution path to locate a user-written *handler* to deal with the error.

Exceptions have problems:

1. They aren't typed.
   Java's checked exceptions provide a small amount of type information, but it's not that helpful compared to a full type system.
   Unchecked exceptions provide no information at all.

1. Because they are handled dynamically, the only way to ensure your program
   won't crash is by testing it through all possible execution paths. A
   statically-typed error management solution can ensure---at compile
   time---that all errors are handled.

1. They do not scale, because it is difficult to know what exceptions a function can throw.
   {{Need to think about this more to make the case.}}

1. Difficult or impossible to retry an operation if it fails.
   Java {{and Scala?}} use the "termination" model of exception handling.
   This assumes the error is so critical there's no way to get back to where the exception occurred.
   If you're performing an operation that you'd like to retry if it fails, exceptions don't help much.

Exceptions were a valiant attempt to produce a consistent error-reporting interface, and they are definitely better than what came before.
But they don't end up solving the problem very well, and you just don't know what you're going to get when you use exceptions.


## ZIO Error Handling

Now we will explore how ZIO enables more powerful, uniform error-handling.

TODO {{Update verbiage now that ZIO section is first}}

- [ZIO Error Handling](#zio-error-handling)
- [Wrapping Legacy Code](#wrapping-legacy-code)

{#zio-error-handling}
### ZIO-First Error Handling

```scala mdoc
// TODO We hide the original implementation of this function, but show this one.
// Is that a problem? Seems unbalanced
val getTemperatureZ =
  getScenario() match
    case Scenario.GPSError =>
      ZIO.fail:
        GpsException()
    case Scenario.NetworkError =>
      // TODO Use a non-exceptional error
      ZIO.fail:
        NetworkException()
    case Scenario.HappyPath =>
      ZIO.succeed:
        "35 degrees"

runScenario(Scenario.HappyPath, getTemperatureZ)
```

```scala mdoc:fail
// TODO make MDoc:fail adhere to line limits?
runScenario(
  Scenario.GPSError,
  getTemperatureZ.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
)
```

```scala mdoc
runScenario(Scenario.GPSError, getTemperatureZ)
```

```scala mdoc:silent
val renderTempZTotal =
  getTemperatureZ.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsException =>
      ZIO.succeed:
        "New GPS Hardware needed"
```

```scala mdoc
runScenario(
  Scenario.GPSError,
  renderTempZTotal
)
```

Now that we have handled all of our errors, we know we are showing the user a sensible message.
Therefore - it would not make sense to retry this rendering.
Note - this is different from retrying the call to get the temperature itself.

```scala mdoc:fail
runScenario(
  Scenario.GPSError,
  renderTempZTotal
    .retryN(10)
)
```

Thanks to the type management provided by our effect library
, the compiler recognizes that this `retryN` can never be used and prevents us from adding it.

{#wrapping-legacy-code}
### Wrapping Legacy Code

If we are unable to re-write the fallible function, we can still wrap the call.

```scala mdoc:silent
val calculateTempWrapped =
  ZIO.attempt:
    calculateTemp()
```

```scala mdoc:silent
val displayTemperatureZWrapped =
  calculateTempWrapped.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsException =>
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

## Unions AKA Sum Types AKA Enums AKA Ors

Note - Avoid official terminology in most prose. Just say "And"/"Or" where appropriate.

Scala 3 automatically aggregates the error types by synthesizing an anonymous sum type from the combined errors.

Functions usually transform the `Answer` from one type to another type.  Errors often aggregate.


Consider 2 error types

```scala
case class UserNotFound()
case class PermissionError()
```

In the type system, the most recent ancestor between them is `Any`.  
Unfortunately, you cannot make any meaningful decisions based on this type.

```mermaid
graph TD;
  UserNotFound-->Nothing;
  PermissionError-->Nothing;
```

We need a more specific way to indicate that our code can fail with either of these types.
The `|` (or) tool provides maximum specificity without the need for inheritance.

*TODO* Figure out how to use pipe symbol in Mermaid

```mermaid
graph TD;
  UserNotFound-->UserNotFound_OR_PermissionError;
  PermissionError-->UserNotFound_OR_PermissionError;
  UserNotFound-->Nothing;
  PermissionError-->Nothing;
```

Often, you do not care that `Nothing` is involved at all.
The mental model can be simply:

```mermaid
graph TD;
  UserNotFound-->UserNotFound_OR_PermissionError;
  PermissionError-->UserNotFound_OR_PermissionError;
```

```scala
case class UserService()
```

```scala
case class User(name: String)
case class SuperUser(name: String)

def getUser(
    userId: String
) =
   if (userId == "morty" || userId = "rick")
     ZIO.succeed:
       User(userId)
   else
     ZIO.fail:
       UserNotFound()

def getSuperUser(
    user: User
) =
  if (user.name = "rick")
    ZIO.succeed:
      SuperUser(user.name)
  else
    ZIO.fail:
      PermissionError()

def loginSuperUser(userId: String) =
  defer:
    val basicUser = getUser(userId).run
    getSuperUser(basicUser).run

```
