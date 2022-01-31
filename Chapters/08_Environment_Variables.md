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
trait HotelApi:
  def cheapest(
      zipCode: String,
      apiKey: String
  ): Either[Error, Hotel]

case class Hotel(name: String)
case class Error(msg: String)
```

```scala mdoc:invisible
import scala.util.{Either, Left, Right}

object HotelApiImpl extends HotelApi:
  def cheapest(
      zipCode: String,
      apiKey: String
  ): Either[Error, Hotel] =
    if (apiKey == "SECRET_API_KEY")
      Right(Hotel("Eddy's Roach Motel"))
    else
      Left(Error("Invalid API Key"))
```

To augment the built-in environment function, we will create a wrapper.

```scala mdoc
def envRequiredUnsafe(
    variable: String
): Either[Error, String] =
  sys
    .env
    .get(variable)
    .toRight(Error("Unconfigured Environment"))
```

`toRight` is an `Option` method that turns the `Option` into an `Either`.

Our business logic now looks like this:

```scala mdoc
def fancyLodgingUnsafe(
    hotelApi: HotelApi
): Either[Error, Hotel] =
  for
    apiKey <- envRequiredUnsafe("API_KEY")
    hotel  <- hotelApi.cheapest("90210", apiKey)
  yield hotel
```

When you look up an Environment Variable, you are accessing information that was _not_ passed into your function as an explicit argument. Now we will simulate running the function with the same arguments in 3 different environments.

**Your Machine:**

```scala mdoc:width=47
fancyLodgingUnsafe(HotelApiImpl)
```

**Collaborator's Machine:**

```scala mdoc:invisible
sys.env.environment = NewDeveloper
```

```scala mdoc:width=47
fancyLodgingUnsafe(HotelApiImpl)
```

**Continuous Integration Server:**

```scala mdoc:invisible
sys.env.environment = CIServer
```

```scala mdoc:width=47
fancyLodgingUnsafe(HotelApiImpl)
```

On your own machine, everything works as expected.
However, your collaborator has a different value stored in this variable, and gets a failure when they execute this code.
Finally, the CI server has not set _any_ value, and fails at runtime.

## Building a Better Way

```scala mdoc:invisible
sys.env.environment = OriginalDeveloper
```

Before looking at the official ZIO implementation of `System`, we will create a less-capable version. We need a `trait` that will indicate what is needed from the environment.
The real implementation is a bit more complex, to handle corner cases.

```scala mdoc
import zio.ZIO

trait System:
  def env(
      variable: String
  ): ZIO[Any, Nothing, Option[String]]
```

Now, our live implementation will wrap our original, unsafe function call.

```scala mdoc
object SystemLive extends System:
  def env(
      variable: String
  ): ZIO[Any, Nothing, Option[String]] =
    ZIO.succeed(sys.env.get("API_KEY"))
```

For easier usage by the caller, we create an accessor.

```scala mdoc
object System:
  def env(
      variable: => String
  ): ZIO[System, Nothing, Option[String]] =
    ZIO.serviceWithZIO[System](_.env(variable))
```

Now if we use this code, our caller's type tells us that it requires a `System` to execute.
This is safe, but it is not the easiest code to use or read.
We then build on first accessor to flatten out the function signature.

```scala mdoc
def envRequired(
    variable: String
): ZIO[System, Error, String] =
  for
    variableAttempt <- System.env(variable)
    res <-
      ZIO
        .fromOption(variableAttempt)
        .mapError(_ =>
          Error("Unconfigured Environment")
        )
  yield res
```
Similarly, we wrap our API in one that leverages ZIO.

```scala mdoc
trait HotelApiZ:
  def cheapest(
      zipCode: String,
      apiKey: String
  ): ZIO[System, Error, Hotel]

object HotelApiZ:
  def cheapest(
      zipCode: String,
      apiKey: String
  ): ZIO[System with HotelApiZ, Error, Hotel] =
    ZIO.serviceWithZIO[HotelApiZ](
      _.cheapest(zipCode, apiKey)
    )

  val live =
    new HotelApiZ:
      def cheapest(
          zipCode: String,
          apiKey: String
      ): ZIO[System, Error, Hotel] =
        ZIO.fromEither(
          HotelApiImpl.cheapest("90210", apiKey)
        )
```
This helps us keep a flat `Error` channel when we write our domain logic.

This was quite a process; where did it get us?
Our fully ZIO-centric, side-effect-free logic looks like this:

```scala mdoc
val fancyLodging
    : ZIO[System with HotelApiZ, Error, Hotel] =
  for
    apiKey <- envRequired("API_KEY")
    hotel  <- HotelApiZ.cheapest("90210", apiKey)
  yield hotel
```

Original, unsafe:

```scala mdoc:nest
def fancyLodgingUnsafe(
    hotelApi: HotelApi
): Either[Error, Hotel] =
  for
    apiKey <- envRequiredUnsafe("API_KEY")
    hotel  <- hotelApi.cheapest("90210", apiKey)
  yield hotel
```

The logic is _identical_ to our original implementation!
The only difference is the result type. 
It now reports the `System` and `HotelApiZ` dependencies of our function.

This is what it looks like in action:

```scala mdoc
import zio.Runtime.default.unsafeRun
import zio.ZLayer
import mdoc.unsafeRunPrettyPrint
```

**Your Machine:**

```scala mdoc:invisible
sys.env.environment = OriginalDeveloper
```

```scala mdoc:fail
unsafeRunPrettyPrint(
  fancyLodging
    .provide(ZLayer.succeed[System](SystemLive))
)
```

**Collaborator's Machine:**
```scala mdoc:invisible
sys.env.environment = NewDeveloper
```

```scala mdoc:silent
// TODO Do this for CI environment too
val collaborater =
  new HotelApiZ:
    def cheapest(
        zipCode: String,
        apiKey: String
    ): ZIO[System, Error, Hotel] =
      ZIO.fromEither(
        HotelApiImpl.cheapest("90210", apiKey)
      )

val colaboraterLayer =
  ZLayer.succeed[System](SystemLive) ++
    ZLayer.succeed(collaborater)
```

```scala mdoc:fail
unsafeRunPrettyPrint(
  fancyLodging().provideLayer(colaboraterLayer)
)
```

**Continuous Integration Server:**

```scala mdoc:invisible
sys.env.environment = CIServer
```

```scala mdoc:fail
unsafeRunPrettyPrint(
  fancyLodging()
    .provide(ZLayer.succeed[System](SystemLive))
)
```

TODO{{The actual line looks the same, which I highlighted as a problem before. How should we indicate that the Environment is different?}}

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

We can now provide this to our logic, for testing both the success and failure cases.

```scala mdoc:fail
unsafeRun(
  fancyLodgingFocused().provide(
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

def fancyLodgingZ(): ZIO[
  zio.System,
  SecurityException,
  Either[Error, Hotel]
] =
  for
    apiKey <- zio.System.env("API_KEY")
  yield HotelApiImpl.cheapest(
    "90210",
    apiKey.get // unsafe! TODO Use either
  )
```

## Exercises

```scala mdoc
import zio.test.TestSystem
import zio.test.TestSystem.Data
// TODO Use real tests once Scala3 & ZIO2 are
// updated
```

X> **Exercise 1:** Create a function will report missing Environment Variables as `NoSuchElementException` failures, instead of an `Option` success case.

```scala mdoc
trait Exercise1:
  def envOrFail(variable: String): ZIO[
    zio.System,
    SecurityException | NoSuchElementException,
    String
  ]
```

```scala mdoc:invisible
object Exercise1Solution extends Exercise1:
  def envOrFail(variable: String): ZIO[
    zio.System,
    SecurityException | NoSuchElementException,
    String
  ] =
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
      .provide(
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
      .provide(
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
```
