# Environment Variables

## Historic Approach

```scala mdoc:invisible
enum Environment:
  case OriginalDeveloper
  case NewDeveloper
  case CIServer

import Environment.*

// TODO Keep an eye out for a _real_ way to
// provide environment variables to this, while
// keeping my .env separate

// This is a _very_ rough way of imitating a real
// ENV access via `sys.env.get`
class Env(var environment: Environment):
  def get(key: String): Option[String] =
    if key == "API_KEY" then
      environment match
        case OriginalDeveloper =>
          Some("SECRET_API_KEY")
        case NewDeveloper =>
          Some("WRONG_API_KEY")
        case CIServer =>
          None
    else
      None

case class Sys(env: Env)

val sys =
  Sys(Env(environment = OriginalDeveloper))
```

Environment Variables are a common way of providing dynamic and/or sensitive data to your running application. A basic use-case looks like this:

```scala mdoc
val apiKey = sys.env.get("API_KEY")
```

This seems rather innocuous; however, it can be an annoying source of problems as your project is built and deployed across different environments. Given this API:

```scala mdoc
trait TravelApi:
  def cheapestHotel(
      zipCode: String,
      apiKey: String
  ): Either[Error, Hotel]

case class Hotel(name: String)
case class Error(msg: String)
```

```scala mdoc:invisible
import scala.util.{Either, Left, Right}

object TravelApiImpl extends TravelApi:
  def cheapestHotel(
      zipCode: String,
      apiKey: String
  ): Either[Error, Hotel] =
    if (apiKey == "SECRET_API_KEY")
      Right(Hotel("Eddy's Roach Motel"))
    else
      Left(Error("Invalid API Key"))
```

Our code could look like this:

```scala mdoc
def findPerfectLodgingUnsafe(
    travelApi: TravelApi
): Either[Error, Hotel] =
  // TODO How many Option/Either tricks are
  // appropriate?
  for
    apiKey <-
      sys
        .env
        .get("API_KEY")
        .toRight(
          Error("Unconfigured Environment")
        )
    res <-
      travelApi.cheapestHotel("90210", apiKey)
  yield res
```

When you look up an Environment Variable, you are accessing information that was _not_ passed in to your function as an explicit argument. Now we will simulate running the exact same function with the same arguments in 3 different environments.

**Your Machine**

```scala mdoc:width=47
findPerfectLodgingUnsafe(TravelApiImpl)
```

**Collaborator's Machine**

```scala mdoc:invisible
sys.env.environment = NewDeveloper
```

```scala mdoc:width=47
findPerfectLodgingUnsafe(TravelApiImpl)
```

**Continuous Integration Server**

```scala mdoc:invisible
sys.env.environment = CIServer
```

```scala mdoc:width=47
findPerfectLodgingUnsafe(TravelApiImpl)
```

On your own machine, everything works as expected. However, your collaborator has a different value stored in this variable, and gets a failure when they execute this code. Finally, the CI server has set _any_ value, and completely blows up at runtime.

## Building a Better Way

```scala mdoc:invisible
sys.env.environment = OriginalDeveloper
```

Before looking at the official ZIO implementation of `System`, we will create a simpler version. We need a `trait` that will indicate what is needed from the environment.

```scala mdoc
import zio.ZIO
trait System:
  def env(
      variable: String
  ): ZIO[Any, Nothing, Option[String]]
```

Now, our live implementation will simply wrap our original, unsafe function call.

```scala mdoc
class SystemLive() extends System:
  def env(
      variable: String
  ): ZIO[Any, Nothing, Option[String]] =
    ZIO.succeed(sys.env.get("API_KEY"))
```

Finally, for easier usage by the caller, we create an accessor.

```scala mdoc
import zio.Has

def env(
    variable: => String
): ZIO[Has[System], Nothing, Option[String]] =
  ZIO.accessZIO(_.get.env(variable))
```

Now if we use this code, our caller's type signature is forced to tell us that it requires a `System` to execute.

```scala mdoc
def anniversaryLodgingSafe(): ZIO[Has[
  System
], Error, Either[Error, Hotel]] =
  for
    apiKeyAttempt <- env("API_KEY")
    apiKey <-
      ZIO
        .fromOption(apiKeyAttempt)
        .mapError(_ =>
          Error("Unconfigured Environment")
        )
  yield TravelApiImpl
    .cheapestHotel("90210", apiKey)
```

This is safe, but it is not the easiest code to read.
TODO {{Consider doing this from the start. Not sure how many different phases to subject the reader to.}}
We can improve the situation by composing our first accessor with some additional transformations.

```scala mdoc
def envRequired(
    variable: => String
): ZIO[Has[System], Error, String] =
  for
    variableAttempt <- env(variable)
    res <-
      ZIO
        .fromOption(variableAttempt)
        .mapError(_ =>
          Error("Unconfigured Environment")
        )
  yield res
```

Using this function, our code becomes more linear and focused.
```scala mdoc
def anniversaryLodgingFocused(): ZIO[Has[
  System
], Error, Either[Error, Hotel]] =
  for
    apiKey <- envRequired("API_KEY")
  yield TravelApiImpl
    .cheapestHotel("90210", apiKey)
```
We can take things one step further, and flatten our two `Error` possibilities into the one failure channel.
```scala mdoc
def anniversaryLodgingFinal()
    : ZIO[Has[System], Error, Hotel] =
  for
    apiKey <- envRequired("API_KEY")
    hotel <-
      ZIO.fromEither(
        TravelApiImpl
          .cheapestHotel("90210", apiKey)
      )
  yield hotel
```

This is what it looks like in action:

```scala mdoc
import zio.Runtime.default.unsafeRun
import zio.ZLayer

unsafeRun(
  anniversaryLodgingSafe().provideLayer(
    ZLayer.succeed[System](SystemLive())
  )
)
```

When constructed this way, it becomes very easy to test. We create a second implementation that accepts test values and serves them to the caller.

```scala mdoc
case class SystemHardcoded(
    environmentVars: Map[String, String]
) extends System:
  def env(
      variable: String
  ): ZIO[Any, Nothing, Option[String]] =
    ZIO.succeed(environmentVars.get(variable))
```

We can now provide this to our logic, for testing both the happy path and failure cases.

```scala mdoc
unsafeRun(
  anniversaryLodgingSafe().provideLayer(
    ZLayer.succeed[System](
      SystemHardcoded(
        Map("API_KEY" -> "Invalid Key")
      )
    )
  )
)
```

## Official ZIO Approach

ZIO provides a more complete `System` API in the `zio.System`

TODO

```scala mdoc
import zio.System

def anniversaryLodgingZ(): ZIO[Has[
  zio.System
], SecurityException, Either[Error, Hotel]] =
  for
    apiKey <- zio.System.env("API_KEY")
  yield TravelApiImpl.cheapestHotel(
    "90210",
    apiKey.get // unsafe!
  )
```

## Exercises

```scala mdoc
import zio.test.environment.TestSystem
import zio.test.environment.TestSystem.Data
// TODO Use real tests once Scala3 & ZIO2 are
// updated
```

X> **Exercise 1:** Create a function will report missing Environment Variables as `NoSuchElementException` failures, instead of an `Option` success case.

```scala mdoc
trait Exercise1:
  def envOrFail(variable: String): ZIO[Has[
    zio.System
  ], SecurityException | NoSuchElementException, String]
```

```scala mdoc:invisible
object Exercise1Solution extends Exercise1:
  def envOrFail(variable: String): ZIO[Has[
    zio.System
  ], SecurityException | NoSuchElementException, String] =
    zio
      .System
      .env(variable)
      .flatMap(
        _.fold(
          ZIO.fail(new NoSuchElementException())
        )(ZIO.succeed(_))
      )
```

```scala mdoc
val exercise1case1 =
  unsafeRun(
    Exercise1Solution
      .envOrFail("key")
      .provideLayer(
        TestSystem.live(
          Data(envs = Map("key" -> "value"))
        )
      )
  )
assert(exercise1case1 == "value")
```

```scala mdoc
val exercise1case2 =
  unsafeRun(
    Exercise1Solution
      .envOrFail("key")
      .catchSome {
        case _: NoSuchElementException =>
          ZIO.succeed("Expected Error")
      }
      .provideLayer(
        TestSystem.live(Data(envs = Map()))
      )
  )

assert(exercise1case2 == "Expected Error")
```

X> **Exercise 2:** Create a function will attempt to parse a value as an Integer and report errors as a `NumberFormatException`.

```scala mdoc
trait Exercise2:
  def envInt(variable: String): ZIO[
    Any,
    NoSuchElementException |
      NumberFormatException,
    Int
  ]
