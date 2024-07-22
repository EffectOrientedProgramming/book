# Failure {#Chapter-Failure}

Error reporting and handling has always been a challenge.
Early solutions often involved setting global flags, which might be overwritten before the error condition was noticed.
People also tried returning improbable values from a function, but this required programmers to know about it and to write code that analyzed the return values.
The biggest problem was psychological: programmers tend to be more interested in the "happy path" where everything works right.
It was too easy to forget or ignore error information.

Exceptions were a big step forward, because they provide:

- Unified error reporting: there's only one way to do it.
- Errors cannot be ignored--they flow upward until caught or displayed on the console with program termination.
- A standardized way to correct problems so that an operation can recover and retry.
- Errors can be handled close to the origin, or generalized by catching them "further out" so that multiple error sources can be managed with a single handler.
- Exception hierarchies allow more general exception handlers to handle multiple exception subtypes.

Exceptions were certainly an improvement over previous attempts to solve the error reporting problem. 
Eventually, however, programmers began encountering pain points. 
As is often the case, this happened as we tried to scale up to create larger and more complex systems. 
The underlying issue was _composability_, which takes smaller parts and assembles them into larger parts.

The main problem with exceptions is that they are not part of the type system.
When exception types are not part of a function signature, you can't know what exceptions you must handle when calling other functions (i.e.: composing). 
You can track down explicitly thrown exceptions by searching for them in the source code.
Even then, built-in exceptions can occur without evidence in the code--for example, divide-by-zero.

Suppose you're handling all exceptions from a library--or at least, the ones you found in the documentation. 
Now a new version of that library comes out.
You upgrade, assuming it must be better.
Unbeknownst to you, the new version quietly added an exception. 
Because exceptions are not part of the type system, the compiler cannot detect the change.
Your code doesn't handle that exception.
Your code was working.
Nobody changed anything in your code.
And yet now it's broken.
Worse, you only find out at runtime, when your system fails.

Languages like C++ and Java tried to solve this problem by adding exception specifications.
This notation adds exception types that may be thrown as part of the function's type signature.
Although this appeared to be a solution, exception specifications are actually a second, shadow type system, independent of the main type system.
All attempts at using exception specifications have failed, and C++ has abandoned exception specifications and adopted the functional approach.

Object-oriented languages have exception hierarchies, which introduces another problem.
Exception hierarchies allow the library programmer to use an exception base type in the exception specification.
This obscures important details; if the exception specification only uses a base type, the compiler cannot enforce coverage of specific exceptions.

When errors are part of the type system, you see all possible errors by looking at the type information.
If a library component adds a new error, it must be reflected in the type signature. 
You immediately know if your code no longer covers all error conditions.

## The Functional Solution

Instead of creating a complex implementation to report and handle errors, the functional approach creates a "return package." 
This is returned from the function, holding either the answer or error information. 
This package is a new type that includes the types of all possible failures.
Now the compiler has enough information to tell you whether you've covered all failure possibilities.

Effects encapsulate the unpredictable parts of a system, so they must be able to express failure.
How is success and failure information encoded into the function return type for an Effect System?
Well, this is what we've been doing whenever we've used `ZIO.succeed` and `ZIO.fail`.
The argument to `succeed` is the successful result value that you want to return.
`succeed` also provides the information that says, "This Effect is OK."
The argument to `fail` is the failure information.
The fact that you are calling `fail` provides the information that says, "Something went wrong in this Effect."

## Failure Types

Although most of the examples in this book use a `String` argument to `fail`, you can give it any type:

```scala 3 mdoc:silent
import zio.*

case object ObjectX
case object ExceptionX extends Exception:
  override def toString: String = "ExceptionX"

def failureTypes(n: Int) =
  n match
    case 0 =>
      ZIO.fail("String fail")
    case 1 =>
      ZIO.fail(ObjectX)
    case _ =>
      ZIO.fail(ExceptionX)
```

`failureTypes` fails in three different ways. 
`case 0` returns a failing `ZIO` object containing a `String`, as we do in most examples in the book.
`case 1` returns an object of type `ObjectX`, demonstrating that you can return any object as your failure information.
The default case returns an `ExceptionX`--but notice that this exception is never thrown.
The exception only provides information about the failure.
This is typically more information than a `String` provides, because the exception is a type.
One reason to return an exception inside a `fail` is if you've caught that exception and want to incorporate this information in the returned Effect.

To make our code easier to read, we have avoided function type signatures in this book, and instead rely on type inference.
The inferred type signature for `failureTypes` includes all three types: `String`, `ObjectX` and `ExceptionX`.
This way, the compiler can verify that all error conditions are handled.

Here we exercise all cases of `failureTypes`:

```scala 3 mdoc:runzio
def run =
  defer:
    val r0 = failureTypes(0).flip.run
    printLine(s"r0: $r0").run
    val r1 = failureTypes(1).flip.run
    printLine(s"r1: $r1").run
    val r2 = failureTypes(2).flip.run
    printLine(s"r2: $r2").run
```

(Describe output)

## Short-Circuiting

The other benefit of handling errors with an Effect System is called _short-circuiting_.
This means that, when an error is encountered, the function stops executing.
Although stopping after you encounter an error seems obvious, in practice it can be hard to enforce.
An Effect System guarantees that you will not execute further code, regardless of how the error occurs.

To demonstrate, we'll use a function that fails if a value `n` is greater than or equal to a `limit` value: 

```scala 3 mdoc:silent
import zio.*

def testLimit(n: Int, limit: Int) =
  println(s"testLimit($n, $limit)")
  if n < limit then
    ZIO.succeed(s"Passed $n")
  else
    println("n >= limit: testLimit failed")
    ZIO.fail(s"Failed at $n")
```

(prose)

```scala 3 mdoc:silent
def shortCircuiting(n: Int) =
  defer:
    val r1 = testLimit(0, n).run
    printLine(s"-> n: $n, r1: $r1").run
    val r2 = testLimit(1, n).run
    printLine(s"-> n: $n, r2: $r2").run
    val r3 = testLimit(2, n).run
    printLine(s"-> n: $n, r3: $r3").run
```

(prose)

```scala 3 mdoc:runzio
def run =
  defer:
    val result0 = shortCircuiting(0).flip.run
    printLine(s"result0: $result0").run
    val result1 = shortCircuiting(1).flip.run
    printLine(s"result1: $result1").run
    val result2 = shortCircuiting(2).flip.run
    printLine(s"result2: $result2").run
```

(prose)

## Reading Temperature

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
    printLine("Getting Temperature")
      .orDie
      .run

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

Suppose an Effect `getTemperature` can fail when it makes a network request.
Let's assume it won't fail by running it in the "happy path":

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = happyPath

def run =
  getTemperature
```

The program succeeds and produces the temperature.
If we run it with a simulated network failure, the program fails:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

// TODO Reduce output here

def run =
  getTemperature
```

Unless we handle failure, the program terminates.
For example, if we try to print something after trying to get the temperature, any unhandled failure prevents it:

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

One solution is to "catch" the failure and transform it into another Effect which can also succeed or fail.
Note that this is a method in the Effect System, *not* the `catch` mechanism of the language:

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

This time, the second Effect runs because we transformed the `getTemperature` failure into a successful result.

With `catchAll` we must handle all types of failure; `getTemperature` can only fail with an Exception.

Let's be more specific in handling different types of failures.
For example, we'll try to catch the `NetworkException`:

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

This produces a compiler warning because `catchAll` does not catch all possible failures.

Here, we handle all failures:

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

Now, even if there is a network or GPS failure, the Effect completes successfully:

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

Since the new `temperatureAppComplete` can no longer fail, there are no failures to "catch."
Trying will result in a compiler error:

```scala 3 mdoc:fail
temperatureAppComplete.catchAll:
  case ex: GpsException =>
    ZIO.succeed:
      "This cannot happen"
```

## Custom Error Types

The failures from `getTemperature` were both `Exception`s, but failures can be any type.

Consider a `check` Effect (implementation hidden) that fails with a custom type `ClimateFailure`:

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
      ZIO
        .succeed:
          "Comfortable Temperature"
        .run
    else
      ZIO
        .fail:
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
The Effect System tracks all failures that occur during a sequence of Effects:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val weatherReportFaulty =
  defer:
    val result = getTemperature.run
    check(result).run
```

To handle all failures for `weatherReportFaulty`, we provide cases for both `Exception` and `ClimateFailure`:

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

All failure types, across the sequence of Effects, are now handled.
As before, the Effect System will produce a compiler error if we miss an error type.

When our combined Effect runs under conditions that are too cold, we get a `ClimateFailure`:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

override val bootstrap = tooCold

def run =
  weatherReport
```

We don't see the `ClimateFailure` error, we only get its `message` as produce by the `catchAll`.

## Short-circuiting Failures

Despite the downsides of throwing `Exception`s, there is a reason it is a common practice.
It is a quick and easy way to stop the function when something goes wrong, without wrapping your logic in `if/else`.

With Effects, we achieve the same behavior without the downsides.

This triggers a `gpsFailure`, causing the *first* Effect to fail:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = gpsFailure

def run =
  weatherReport
```

The program fails with the GPS failure, and the `check` Effect does not run.

Short-circuiting is an essential part of user-friendly Effect Systems.
With it, we can write a linear sequence of fallible expressions, while tracking all possible failures.

## Handling Thrown Exceptions

So far our example Effects have **returned** `Exception`s to indicate failure, but you may have legacy code or external libraries which **throw** `Exception`s instead.
In these situations there are ways to wrap the `Exception`-throwing code so that we achieve our preferred style of returning `Exception`s.

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

The `getTemperatureOrThrow` function can fail by throwing an `Exception`.
If we call this function from an Effect, a failure causes the program to fail:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  ZIO.succeed:
    getTemperatureOrThrow()
```
Despite our claim that this Effect `succeed`s, it crashes with a defect.
When we call side-effecting code, the Effect System can't warn us about the potential failure.

The solution is to use `ZIO.attempt`, which captures thrown `Exception`s as the error type of the Effect:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val safeTemperatureApp =
  ZIO.attempt:
    getTemperatureOrThrow()
```

Now you can use any of the failure handling mechanisms in Effects to deal with the failure:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = networkFailure

def run =
  safeTemperatureApp.orElse:
    ZIO.succeed:
      "Could not get temperature"
```

Here we handle the Effect's failure with a fallback Effect, which succeeds.

Thrown `Exception`s are inherently unpredictable, so it is preferable to encapsulate all exception-throwing functions into Effects.
This makes that unpredictability clear and provides mechanisms for handling possible failures. 
