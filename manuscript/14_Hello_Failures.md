# Hello Failures

If you are not interested in the discouraged ways to handle errors, and just want to see the ZIO approach, jump down to 
[ZIO Error Handling](#zio-error-handling)

## Historic approaches to Error-handling
In the past, some programs have thrown exceptions to indicate failures.
Imagine a program that displays the local temperature the user based on GPS position and a network call. There are distinct levels of problems in any given program. They require different types of handling by the programmer.

```text
Temperature: 30 degrees
```

```scala
class GpsException()     extends RuntimeException
class NetworkException() extends RuntimeException

enum Scenario:
  case Success,
    NetworkError,
    GPSError

def displayTemperature(
    behavior: Scenario
): String =
  if (behavior == Scenario.GPSError)
    throw new GpsException()
  else if (behavior == Scenario.NetworkError)
    throw new NetworkException()
  else
    "35 degrees"
```

```scala
def currentTemperatureUnsafe(
    behavior: Scenario
): String =
  "Temperature: " + displayTemperature(behavior)

currentTemperatureUnsafe(Scenario.Success)
// res0: String = "Temperature: 35 degrees"
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
This can take many forms.
If we don't make any attempt to handle our problem, the whole program blows up and shows the gory details to the user.

```scala
// Note - Can't make this output prettier/simpler because it's *not* using ZIO
currentTemperatureUnsafe(Scenario.NetworkError)
// repl.MdocSession$MdocApp$NetworkException
// 	at repl.MdocSession$MdocApp.displayTemperature(14_Hello_Failures.md:25)
// 	at repl.MdocSession$MdocApp.currentTemperatureUnsafe(14_Hello_Failures.md:35)
// 	at repl.MdocSession$MdocApp.$init$$$anonfun$1(14_Hello_Failures.md:46)
```

We could take the bare-minimum approach of catching the `Exception` and returning `null`:

```scala
def currentTemperatureNull(
    behavior: Scenario
): String =
  try
    "Temperature: " +
      displayTemperature(behavior)
  catch
    case (ex: RuntimeException) =>
      "Temperature: " + null

currentTemperatureNull(Scenario.NetworkError)
// res1: String = "Temperature: null"
```

This is *slightly* better, as the user can at least see the outer structure of our UI element, but it still leaks out code-specific details world.

Maybe we could fallback to a `sentinel` value, such as `0` or `-1` to indicate a failure?

```scala
def currentTemperature(
    behavior: Scenario
): String =
  try
    "Temperature: " +
      displayTemperature(behavior)
  catch
    case (ex: RuntimeException) =>
      "Temperature: -1 degrees"

currentTemperature(Scenario.NetworkError)
// res2: String = "Temperature: -1 degrees"
```

Clearly, this isn't acceptable, as both of these common sentinel values are valid temperatures.
We can take a more honest and accurate approach in this situation.

```scala
def currentTemperature(
    behavior: Scenario
): String =
  try
    "Temperature: " +
      displayTemperature(behavior)
  catch
    case (ex: RuntimeException) =>
      "Temperature Unavailable"

currentTemperature(Scenario.NetworkError)
// res3: String = "Temperature Unavailable"
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```scala
def currentTemperature(
    behavior: Scenario
): String =
  try
    "Temperature: " +
      displayTemperature(behavior)
  catch
    case (ex: NetworkException) =>
      "Network Unavailable"
    case (ex: GpsException) =>
      "GPS problem"

currentTemperature(Scenario.NetworkError)
// res4: String = "Network Unavailable"
currentTemperature(Scenario.GPSError)
// res5: String = "GPS problem"
```

Wonderful!
We have specific messages for all relevant error cases. However, this still suffers from downsides that become more painful as the codebase grows.

- The signature of `currentTemperature` does not alert us that it might fail
- If we realize it can fail, we must dig through the implementation to discover the multiple failure values
- We never have certainty about the failure paths of our full application, or any subset of it.

{{ TODO Tear apart exceptions more }}

Encountering an error during a function call generally means two things:

1. You can't continue executing the function in the normal fashion.

2. You can't return a normal result.

Many languages use *exceptions* for handling errors.
An exception *throws* out of the current execution path to locate a user-written *handler* to deal with the error.
There are two goals for exceptions:

1. Separate error-handling code from "success-path" code, so the success-path code is easier to understand and reason about.

2. Reduce redundant error-handling code by handling associated errors in a single place.

Exceptions have problems:

1. They can be "swallowed."
   Just because code throws an exception, there's no guarantee that issue will be dealt with.

1. They can lose important information.
   Once an exception is caught, it is considered to be "handled," and the program doesn't need to retain the failure information.

1. They aren't typed.
   Java's checked exceptions provide a small amount of type information, but it's not that helpful compared to a full type system.
   Unchecked exceptions provide no information at all.

1. Because they are handled dynamically, the only way to ensure your program
   won't crash is by testing it through all possible execution paths. A
   statically-typed error management solution can ensure---at compile
   time---that all errors are handled.

1. They don't scale.
   {{Need to think about this more to make the case.}}

1. Hard to reason about. {{Also need to make this case}}

1. Difficult or impossible to retry an operation if it fails.
   Java {{and Scala?}} use the "termination" model of exception handling.
   This assumes the error is so critical there's no way to get back to where the exception occurred.
   If you're performing an operation that you'd like to retry if it fails, exceptions don't help much.

Exceptions were a valiant attempt to produce a consistent error-reporting interface, and they are definitely better than what's in C.
But they don't end up solving the problem very well, and you just don't know what you're going to get when you use exceptions.


### What's wrong with Try?

### ADTS as another step forward

## ZIO Error Handling

Now we will explore how ZIO enables more powerful, uniform error-handling.

TODO {{Update verbiage now that ZIO section is first}}

- [ZIO Error Handling](#zio-error-handling)
- [Wrapping Legacy Code](#wrapping-legacy-code)

### ZIO-First Error Handling

```scala
import zio.ZIO
import mdoc.unsafeRunPrettyPrint

def getTemperatureZ(behavior: Scenario): ZIO[
  Any,
  GpsException | NetworkException,
  String
] =
  if (behavior == Scenario.GPSError)
    ZIO.fail(new GpsException())
  else if (behavior == Scenario.NetworkError)
    // TODO Use a non-exceptional error
    ZIO.fail(new NetworkException())
  else
    ZIO.succeed("30 degrees")

unsafeRunPrettyPrint(
  getTemperatureZ(Scenario.Success)
)
// res6: String = "30 degrees"
```

```scala
unsafeRunPrettyPrint(
  getTemperatureZ(Scenario.Success).catchAll {
    case ex: NetworkException =>
      ZIO.succeed("Network Unavailable")
  }
)
// error: 
// match may not be exhaustive.
// 
// It would fail on pattern case: _: GpsException
//
```

TODO Demonstrate ZIO calculating the error types without an explicit annotation being provided

```scala
unsafeRunPrettyPrint(
  getTemperatureZ(Scenario.GPSError)
)
// res8: String = "repl.MdocSession$MdocApp$GpsException"
```

### Wrapping Legacy Code

If we are unable to re-write the fallible function, we can still wrap the call
We are re-using the  `displayTemperature`

{{TODO }}

```scala
import zio.{Task, ZIO}
```

```scala
def displayTemperatureZWrapped(
    behavior: Scenario
): ZIO[Any, Nothing, String] =
  ZIO
    .attempt(displayTemperature(behavior))
    .catchAll {
      case ex: NetworkException =>
        ZIO.succeed("Network Unavailable")
      case ex: GpsException =>
        ZIO.succeed("GPS problem")
    }
```

```scala
unsafeRunPrettyPrint(
  displayTemperatureZWrapped(Scenario.Success)
)
// res9: String = "35 degrees"
```

```scala
unsafeRunPrettyPrint(
  displayTemperatureZWrapped(
    Scenario.NetworkError
  )
)
// res10: String = "Network Unavailable"
```

This is decent, but does not provide the maximum possible guarantees. Look at what happens if we forget to handle one of our errors.

```scala
def getTemperatureZGpsGap(
    behavior: Scenario
): ZIO[Any, Nothing, String] =
  ZIO
    .attempt(displayTemperature(behavior))
    .catchAll { case ex: NetworkException =>
      ZIO.succeed("Network Unavailable")
    }
```

```scala
unsafeRunPrettyPrint(
  getTemperatureZGpsGap(Scenario.GPSError)
)
// res11: String = "Defect: GpsException"
```

The compiler does not catch this bug, and instead fails at runtime. 
Take extra care when interacting with legacy code, since we cannot automatically recognize these situations at compile time.
We have 2 options in these situations.

First, we can provide a fallback case that will report anything we missed:
```scala
def getTemperatureZWithFallback(
    behavior: Scenario
): ZIO[Any, Nothing, String] =
  ZIO
    .attempt(displayTemperature(behavior))
    .catchAll {
      case ex: NetworkException =>
        ZIO.succeed("Network Unavailable")
      case other =>
        ZIO.succeed("Unexpected error: " + other)
    }
```

```scala
unsafeRunPrettyPrint(
  getTemperatureZWithFallback(Scenario.GPSError)
)
// res12: String = "Unexpected error: repl.MdocSession$MdocApp$GpsException"
```

This lets us avoid the most egregious gaps in functionality, but it does not take full advantage of ZIO's type-safety.
```scala
def getTemperatureZAndFlagUnhandled(
    behavior: Scenario
): ZIO[Any, GpsException, String] =
  ZIO
    .attempt(displayTemperature(behavior))
    .catchSome { case ex: NetworkException =>
      ZIO.succeed("Network Unavailable")
    }
    // TODO Eh, find a better version of this.
    .mapError(_.asInstanceOf[GpsException])
```

```scala
unsafeRunPrettyPrint(
  getTemperatureZAndFlagUnhandled(
    Scenario.GPSError
  )
)
// res13: String = "repl.MdocSession$MdocApp$GpsException"
```


{{TODO show catchSome}}

## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/hello_failures/BadTypeManagement.scala
```scala
package hello_failures

import zio.ZIO

object BadTypeManagement
    extends zio.ZIOAppDefault:
  val logic: ZIO[Any, Exception, String] =
    for
      _ <- ZIO.debug("ah")
      result <-
        failable(1).catchAll {
          case ex: Exception =>
            ZIO.fail(ex)
          case ex: String =>
            ZIO.succeed(
              "recovered string error: " + ex
            )
        }
      _ <- ZIO.debug(result)
    yield result
  def run = logic

  def failable(
      path: Int
  ): ZIO[Any, Exception | String, String] =
    if (path < 0)
      ZIO.fail(new Exception("Negative path"))
    else if (path > 0)
      ZIO.fail("Too big")
    else
      ZIO.succeed("just right")
end BadTypeManagement

```


### experiments/src/main/scala/hello_failures/KeepSuccesses.scala
```scala
package hello_failures

import zio.Console.printLine
import zio.ZIO

object KeepSuccesses extends zio.ZIOAppDefault:
  val allCalls =
    List("a", "b", "large payload", "doomed")

  case class GoodResponse(payload: String)
  case class BadResponse(payload: String)

  val initialRequests =
    allCalls.map(fastUnreliableNetworkCall)

  val logic =
    for
      results <-
        ZIO.collectAllSuccesses(
          initialRequests.map(
            _.tapError(e =>
              printLine("Error: " + e)
            )
          )
        )
      _ <- printLine(results)
    yield ()

  val moreStructuredLogic =
    for
      results <-
        ZIO.partition(allCalls)(
          fastUnreliableNetworkCall
        )
      _ <-
        results match
          case (failures, successes) =>
            for
              _ <-
                ZIO.foreach(failures)(e =>
                  printLine(
                    "Error: " + e +
                      ". Should retry on other server."
                  )
                )
              recoveries <-
                ZIO.collectAllSuccesses(
                  failures.map(failure =>
                    slowMoreReliableNetworkCall(
                      failure.payload
                    ).tapError(e =>
                      printLine(
                        "Giving up on: " + e
                      )
                    )
                  )
                )
              _ <-
                printLine(
                  "All successes: " +
                    (successes ++ recoveries)
                )
            yield ()
    yield ()

  val logicSpecific =
    ZIO.collectAllWith(initialRequests)(
      _.payload.contains("a")
    )

  def run =
//      logic
    moreStructuredLogic

  def fastUnreliableNetworkCall(input: String) =
    if (input.length < 5)
      ZIO.succeed(GoodResponse(input))
    else
      ZIO.fail(BadResponse(input))

  def slowMoreReliableNetworkCall(
      input: String
  ) =
    if (input.contains("a"))
      ZIO.succeed(GoodResponse(input))
    else
      ZIO.fail(BadResponse(input))
end KeepSuccesses

```


### experiments/src/main/scala/hello_failures/OrDie.scala
```scala
package hello_failures

import zio.ZIO

object OrDie extends zio.ZIOAppDefault:
  val logic =
    for _ <- failable(-1).orDie
    yield ()

  def run = logic

  def failable(
      path: Int
  ): ZIO[Any, Exception, String] =
    if (path < 0)
      ZIO.fail(new Exception("Negative path"))
//    else if (path > 0)
//      ZIO.fail("Too big")
    else
      ZIO.succeed("just right")

```


### experiments/src/main/scala/hello_failures/catching.scala
```scala
package hello_failures

import zio.*
import zio.Console.*
import hello_failures.file
import java.io.IOException

def standIn: ZIO[Any, IOException, Unit] =
  printLine("Im a stand-in")

object catching extends zio.ZIOAppDefault:

  val logic = loadFile("TargetFile")

  def run =
    logic
      .catchAll(_ =>
        println("Error Caught")
        loadBackupFile()
      )
      .exitCode

// standIn.exitCode

```


### experiments/src/main/scala/hello_failures/fallback.scala
```scala
package hello_failures

import zio.*
import zio.Console
import scala.util.Random
// A useful way of dealing with errors is by
// using the
// `orElse()` method.

case class file(name: String)

def loadFile(fileName: String) =
  if (Random.nextBoolean())
    println("First Attempt Successful")
    ZIO.succeed(file(fileName))
  else
    println("First Attempt Not Successful")
    ZIO.fail("File not found")

def loadBackupFile() =
  println("Backup file used")
  ZIO.succeed(file("BackupFile"))

object fallback extends zio.ZIOAppDefault:

  // orElse is a combinator that can be used to
  // handle
  // effects that can fail.

  def run =
    val loadedFile: UIO[file] =
      loadFile("TargetFile")
        .orElse(loadBackupFile())
    loadedFile.exitCode

```


### experiments/src/main/scala/hello_failures/folding.scala
```scala
package hello_failures

import zio.*
import zio.Console.*
import hello_failures.file
import hello_failures.standIn

object folding extends ZIOAppDefault:
// When applied to ZIO, fold() allows the
  // programmer to handle both failure
// and success at the same time.
// ZIO's fold method can be broken into two
  // pieces: fold(), and foldM()
// fold() supplied a non-effectful handler, why
  // foldM() applies an effectful handler.

  val logic = loadFile("targetFile")

  def run =
    logic
      .foldZIO(
        _ => loadBackupFile(),
        _ =>
          printLine(
            "The file opened on first attempt!"
          )
      ) // Effectful handling
      .exitCode
end folding

```


### experiments/src/main/scala/hello_failures/value.scala
```scala
package hello_failures

import zio.*

object value:
  // Either and Absolve take ZIO types and
  // 'surface' or 'submerge'
  // the error.

  // Either takes an ZIO[R, E, A] and produces an
  // ZIO[R, Nothing, Either[E,A]]
  // The error is 'surfaced' by making a
  // non-failing ZIO that returns an Either.

  // Absolve takes an ZIO[R, Nothing,
  // Either[E,A]], and returns a ZIO[R,E,A]
  // The error is 'submerged', as it is pushed
  // from an either into a ZIO.

  val zEither: UIO[Either[String, Int]] =
    ZIO.fail("Boom").either

  // IO.fail("Boom") is naturally type
  // ZIO[R,String,Int], but is
  // converted into type UIO[Either[String, Int]

  def sqrt(
      input: UIO[Double]
  ): IO[String, Double] =
    ZIO.absolve(
      input.map(value =>
        if (value < 0.0)
          Left("Value must be >= 0.0")
        else
          Right(Math.sqrt(value))
      )
    )
end value

// The Left-Right statements naturally from an
// 'either' of type either[String, Double].
// the ZIO.absolve changes the either into an
// ZIO of type IO[String, Double]

```

            