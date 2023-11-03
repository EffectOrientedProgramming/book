# Configuration

Changing things based on the running environment.

## CLI Params

## Config Files

## ZIO Config

## Environment Variables

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
case class Hotel(name: String)
case class Error(msg: String)
```

```scala mdoc:invisible
import scala.util.{Either, Left, Right}

case class HotelApi():
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
fancyLodgingUnsafe(HotelApi())
```

**Collaborator's Machine:**

```scala mdoc:invisible
sys.env.environment = NewDeveloper
```

```scala mdoc:width=47
fancyLodgingUnsafe(HotelApi())
```

**Continuous Integration Server:**

```scala mdoc:invisible
sys.env.environment = CIServer
```

```scala mdoc:width=47
fancyLodgingUnsafe(HotelApi())
```

On your own machine, everything works as expected.
However, your collaborator has a different value stored in this variable, and gets a failure when they execute this code.
Finally, the CI server has not set _any_ value, and fails at runtime.

## Building a Better Way

```scala mdoc:invisible
sys.env.environment = OriginalDeveloper
```

ZIO has a full `System` implementation of, but we will consider just 1 function for the moment.

```scala mdoc
def envZ(
    variable: String
): ZIO[Any, Nothing, Option[String]] =
  ZIO.succeed(sys.env.get("API_KEY"))
```
This merely wraps our original function call.
This is safe, but it is not the easiest code to use or read.
We want to convert an empty `Option` into an error state.

```scala mdoc
object SystemStrict:
  val live: ZLayer[Any, Nothing, SystemStrict] =
    ZLayer.fromZIO(
      defer {
        SystemStrict()
      }
    )

case class SystemStrict():
  def envRequired(
      variable: String
  ): ZIO[Any, Error, String] =
    defer {
      val variableAttempt = envZ(variable).run
      ZIO
        .fromOption(variableAttempt)
        .mapError(_ =>
          Error("Missing value for: " + variable)
        )
        .run
    }
```

Similarly, we wrap our API in one that leverages ZIO.

```scala mdoc
case class HotelApiZ(
    system: SystemStrict,
    hotelApi: HotelApi
):
  def cheapest(
      zipCode: String
  ): ZIO[Any, Error, Hotel] =
    defer {
      val apiKey =
        system.envRequired("API_KEY").run
      ZIO
        .fromEither(
          hotelApi.cheapest(zipCode, apiKey)
        )
        .run
    }

object HotelApiZ:
  val live: ZLayer[
    SystemStrict with HotelApi,
    Nothing,
    HotelApiZ
  ] =
    ZLayer.fromZIO(
      defer {
        HotelApiZ(
          ZIO.service[SystemStrict].run,
          ZIO.service[HotelApi].run
        )
      }
    )
```
This helps us keep a flat `Error` channel when we write our domain logic.

This was quite a process; where did it get us?
Our fully ZIO-centric, side-effect-free logic looks like this:


```scala mdoc
// TODO This produces large, wide output that does not adhere to the width of the page.
// TODO This has fallen out of sync with the "identical" code below
// TODO Make this a case class?

def fancyLodging(
    hotelApiZ: HotelApiZ
): ZIO[Any, Error, Hotel] =
  hotelApiZ.cheapest("90210")
```

Original, unsafe:

```scala mdoc:nest:fail
def fancyLodgingUnsafe(
    hotelApi: HotelApi
): Either[Error, Hotel] =
  hotelApi.cheapest("90210")
```

The logic is _identical_ to our original implementation!
The only difference is the result type.

This is what it looks like in action:

**Your Machine:**

```scala mdoc:invisible
sys.env.environment = OriginalDeveloper
```

```scala mdoc:silent
// TODO Do this for CI environment too
// TODO "originalAuthor" Don't know why it's called that?
val originalAuthor = HotelApiZ.live
```

```scala mdoc
val logic =
  defer {
    fancyLodging(ZIO.service[HotelApiZ].run)
  }
runDemo(
  logic.provide(
    SystemStrict.live,
    ZLayer.succeed(HotelApi()),
    originalAuthor
  )
)
```

**Collaborator's Machine:**
```scala mdoc:invisible
sys.env.environment = NewDeveloper
```

```scala mdoc:silent
// TODO Do this for CI environment too
val collaborater = HotelApiZ.live

val colaboraterLayer = collaborater
```

```scala mdoc
runDemo(
  defer {
    fancyLodging(ZIO.service[HotelApiZ].run)
  }.provide(
    SystemStrict.live,
    ZLayer.succeed(HotelApi()),
    collaborater
  )
)
```

**Continuous Integration Server:**

```scala mdoc:invisible
sys.env.environment = CIServer
```

```scala mdoc:silent
val ci = HotelApiZ.live
```

```scala mdoc
runDemo(
  defer {
    fancyLodging(ZIO.service[HotelApiZ].run)
  }.provide(
    SystemStrict.live,
    ZLayer.succeed(HotelApi()),
    ci
  )
)
```

TODO{{The actual line looks the same, which I highlighted as a problem before. How should we indicate that the Environment is different?}}

When constructed this way, it becomes very easy to test.
We create a second implementation that accepts test values and serves them to the caller.
TODO{{Reorder things so that the official ZIO TestSystem is used.}}

## Official ZIO Approach

ZIO provides a more complete `System` API in the `zio.System`.
This is always available as a standard service from the ZIO runtime.

TODO

```scala mdoc
def fancyLodgingBuiltIn(
    hotelApiZ: HotelApiZ
): ZIO[Any, SecurityException | Error, Hotel] =
  defer {
    val apiKey = zio.System.env("API_KEY").run
    hotelApiZ
      .cheapest(
        apiKey.get // unsafe! TODO Use either
      )
      .run
  }
```

## Exercises

```todo
import zio.test.TestSystem
import zio.test.TestSystem.Data
// TODO Use real tests once Scala3 & ZIO2 are
// updated
```

X> **Exercise 1:** Create a function will report missing Environment Variables as `NoSuchElementException` failures, instead of an `Option` success case.

```todo
trait Exercise1:
  def envOrFail(variable: String): ZIO[
    zio.System,
    SecurityException | NoSuchElementException,
    String
  ]
```

```todo
object Exercise1Solution extends Exercise1:
  def envOrFail(variable: String): ZIO[
    zio.System,
    SecurityException | NoSuchElementException,
    String
  ] =
    // TODO Direct instead of flatmap
    zio
      .System
      .env(variable)
      .flatMap(
        _.fold(
          ZIO.fail(new NoSuchElementException())
        )(ZIO.succeed(_))
      )
```

```todo
import zio.test.*

runSpec(
  defer {
    val res = Exercise1Solution
      .envOrFail("key")
      .provide(
        TestSystem.live(
          Data(envs = Map("key" -> "value"))
        )
      )
      .run
    
    assertTrue(res == "value")
  }
)

```

```todo
import zio.test.*

runSpec(
  defer {
    val res =
      Exercise1Solution
        .envOrFail("key")
        .catchSome {
          case _: NoSuchElementException =>
            ZIO.succeed("Expected Error")
        }
        .provide(
          TestSystem.live(Data(envs = Map()))
        )
        .run

      assertTrue(res == "Expected Error")
    }
)
```

X> **Exercise 2:** Create a function will attempt to parse a value as an Integer and report errors as a `NumberFormatException`.

```scala mdoc
trait Exercise2:
  def envInt(variable: String): ZIO[
    Any,
    NoSuchElementException |
      NumberFormatException,
    Int
  ] = ???
```


```scala mdoc:invisible
println(new Exercise2 {})
println(
  fancyLodgingBuiltIn(
    HotelApiZ(SystemStrict(), HotelApi())
  )
)
```
