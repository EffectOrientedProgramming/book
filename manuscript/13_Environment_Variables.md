# Environment Variables

## Historic Approach


Environment Variables are a common way of providing dynamic and/or sensitive data to your running application. A basic use-case looks like this:

```scala
val apiKey = sys.env.get("API_KEY")
// apiKey: Option[String] = Some(value = "SECRET_API_KEY")
```

This seems rather innocuous; however, it can be an annoying source of problems as your project is built and deployed across different environments. Given this API:

```scala
trait HotelApi:
  def cheapest(
      zipCode: String,
      apiKey: String
  ): Either[Error, Hotel]

case class Hotel(name: String)
case class Error(msg: String)
```


To augment the built-in environment function, we will create a wrapper.

```scala
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

```scala
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

```scala
fancyLodgingUnsafe(HotelApiImpl)
// res0: Either[Error, Hotel] = Right(
//   value = Hotel(name = "Eddy's Roach Motel")
// )
```

**Collaborator's Machine:**


```scala
fancyLodgingUnsafe(HotelApiImpl)
// res2: Either[Error, Hotel] = Left(
//   value = Error(msg = "Invalid API Key")
// )
```

**Continuous Integration Server:**


```scala
fancyLodgingUnsafe(HotelApiImpl)
// res4: Either[Error, Hotel] = Left(
//   value = Error(
//     msg = "Unconfigured Environment"
//   )
// )
```

On your own machine, everything works as expected.
However, your collaborator has a different value stored in this variable, and gets a failure when they execute this code.
Finally, the CI server has not set _any_ value, and fails at runtime.

## Building a Better Way


Before looking at the official ZIO implementation of `System`, we will create a less-capable version. We need a `trait` that will indicate what is needed from the environment.
The real implementation is a bit more complex, to handle corner cases.

```scala
import zio.ZIO

trait System:
  def env(
      variable: String
  ): ZIO[Any, Nothing, Option[String]]
```

Now, our live implementation will wrap our original, unsafe function call.
For easier usage by the caller, we also create an accessor.

```scala
import zio.ZLayer

object System:
  object Live extends System:
    def env(
        variable: String
    ): ZIO[Any, Nothing, Option[String]] =
      ZIO.succeed(sys.env.get("API_KEY"))

  val live: ZLayer[Any, Nothing, System] =
    ZLayer.succeed(Live)

  def env(
      variable: => String
  ): ZIO[System, Nothing, Option[String]] =
    ZIO.serviceWithZIO[System](_.env(variable))
```

Now if we use this code, our caller's type tells us that it requires a `System` to execute.
This is safe, but it is not the easiest code to use or read.
We then build on first accessor to flatten out the function signature.

```scala
trait SystemStrict:
  def envRequired(
      variable: String
  ): ZIO[Any, Error, String]

object SystemStrict:
  val live
      : ZLayer[System, Nothing, SystemStrict] =
    ZLayer
      .fromZIO(ZIO.service[System].map(Live(_)))

  def envRequired(
      variable: String
  ): ZIO[SystemStrict, Error, String] =
    ZIO.serviceWithZIO[SystemStrict](
      _.envRequired(variable)
    )

  case class Live(system: System)
      extends SystemStrict:
    def envRequired(
        variable: String
    ): ZIO[Any, Error, String] =
      for
        variableAttempt <- system.env(variable)
        res <-
          ZIO
            .fromOption(variableAttempt)
            .mapError(_ =>
              Error("Unconfigured Environment")
            )
      yield res
end SystemStrict
```
     
Similarly, we wrap our API in one that leverages ZIO.

```scala
trait HotelApiZ:
  def cheapest(
      zipCode: String
  ): ZIO[Any, Error, Hotel]

object HotelApiZ:
  def cheapest(zipCode: String): ZIO[
    SystemStrict with HotelApiZ,
    Error,
    Hotel
  ] =
    ZIO.serviceWithZIO[HotelApiZ](
      _.cheapest(zipCode)
    )

  case class Live(system: SystemStrict)
      extends HotelApiZ:
    def cheapest(
        zipCode: String
    ): ZIO[Any, Error, Hotel] =
      for
        apiKey <- system.envRequired("API_KEY")
        res <-
          ZIO.fromEither(
            HotelApiImpl
              .cheapest(zipCode, apiKey)
          )
      yield res

  val live: ZLayer[
    SystemStrict,
    Nothing,
    HotelApiZ
  ] =
    ZLayer.fromZIO(
      ZIO.service[SystemStrict].map(Live(_))
    )
end HotelApiZ
```
This helps us keep a flat `Error` channel when we write our domain logic.

This was quite a process; where did it get us?
Our fully ZIO-centric, side-effect-free logic looks like this:

```scala
val fancyLodging: ZIO[
  SystemStrict with HotelApiZ,
  Error,
  Hotel
] =
  for hotel <- HotelApiZ.cheapest("90210")
  yield hotel
// fancyLodging: ZIO[SystemStrict & HotelApiZ, Error, Hotel] = OnSuccess(
//   trace = "repl.MdocSession.MdocApp.fancyLodging(13_Environment_Variables.md:262)",
//   first = OnSuccess(
//     trace = "repl.MdocSession.MdocApp.HotelApiZ.cheapest(13_Environment_Variables.md:226)",
//     first = Sync(
//       trace = "repl.MdocSession.MdocApp.HotelApiZ.cheapest(13_Environment_Variables.md:226)",
//       eval = zio.ZIOCompanionVersionSpecific$$Lambda$14485/692886787@224b9f5a
//     ),
//     successK = zio.ZIO$$$Lambda$14455/964190642@1dcf8b9d
//   ),
//   successK = zio.ZIO$$Lambda$14440/941403246@27c9e058
// )
```

Original, unsafe:

```scala
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

```scala
import zio.ZLayer
import mdoc.unsafeRunPrettyPrint
import mdoc.unsafeRunPrettyPrintValue
```

**Your Machine:**


```scala
// TODO Do this for CI environment too
val originalAuthor = HotelApiZ.live
```

```scala
unsafeRunPrettyPrint(
  fancyLodging.provideLayer(
    System.live >>> SystemStrict.live >+>
      originalAuthor
  )
)
// Hotel(Eddy's Roach Motel)
```

**Collaborator's Machine:**

```scala
// TODO Do this for CI environment too
val collaborater = HotelApiZ.live

val colaboraterLayer =
  collaborater ++ System.live
```

```scala
unsafeRunPrettyPrint(
  fancyLodging.provideLayer(
    System.live >>> SystemStrict.live >+>
      collaborater
  )
)
// Error(Invalid API Key)
```

**Continuous Integration Server:**


```scala
val ci = HotelApiZ.live
```

```scala
unsafeRunPrettyPrint(
  fancyLodging.provideLayer(
    System.live >>> SystemStrict.live >+> ci
  )
)
// Error(Unconfigured Environment)
```

TODO{{The actual line looks the same, which I highlighted as a problem before. How should we indicate that the Environment is different?}}

When constructed this way, it becomes very easy to test. We create a second implementation that accepts test values and serves them to the caller.

```scala
case class SystemHardcoded(
    environmentVars: Map[String, String]
) extends System:
  def env(
      variable: String
  ): ZIO[Any, Nothing, Option[String]] =
    ZIO.succeed(environmentVars.get(variable))
```

We can now provide this to our logic, for testing both the success and failure cases.

```scala
val testApiLayer =
  ZLayer.succeed[System](
    SystemHardcoded(
      Map("API_KEY" -> "Invalid Key")
    )
  ) >>> SystemStrict.live >+> HotelApiZ.live
```

```scala
import mdoc.unsafeRunPrettyPrint

unsafeRunPrettyPrint(
  fancyLodging.provide(testApiLayer)
)
// Error(Invalid API Key)
```

## Official ZIO Approach

ZIO provides a more complete `System` API in the `zio.System`

TODO

```scala
import zio.System

def fancyLodgingZ(): ZIO[
  zio.System,
  SecurityException,
  Either[Error, Hotel]
] =
  for apiKey <- zio.System.env("API_KEY")
  yield HotelApiImpl.cheapest(
    "90210",
    apiKey.get // unsafe! TODO Use either
  )
```

## Exercises

```scala
import zio.test.TestSystem
import zio.test.TestSystem.Data
```

X> **Exercise 1:** Create a function will report missing Environment Variables as `NoSuchElementException` failures, instead of an `Option` success case.

```scala
trait Exercise1:
  def envOrFail(variable: String): ZIO[
    zio.System,
    SecurityException | NoSuchElementException,
    String
  ]
```


```scala
val exercise1case1 =
  unsafeRunPrettyPrintValue(
    Exercise1Solution
      .envOrFail("key")
      .provide(
        TestSystem.live(
          Data(envs = Map("key" -> "value"))
        )
      )
  )
// value
// exercise1case1: String = "value"
assert(exercise1case1 == "value")
```

```scala
val exercise1case2 =
  unsafeRunPrettyPrintValue(
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
// Expected Error
// exercise1case2: String = "Expected Error"

assert(exercise1case2 == "Expected Error")
```

X> **Exercise 2:** Create a function will attempt to parse a value as an Integer and report errors as a `NumberFormatException`.

```scala
trait Exercise2:
  def envInt(variable: String): ZIO[
    Any,
    NoSuchElementException |
      NumberFormatException,
    Int
  ]
```
