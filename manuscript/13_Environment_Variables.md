# Environment Variables

## Historic Approach


Environment Variables are a common way of providing dynamic and/or sensitive data to your running application. A basic use-case looks like this:

```scala
val apiKey = sys.env.get("API_KEY")
// apiKey: Option[String] = Some(value = "SECRET_API_KEY")
```

This seems rather innocuous; however, it can be an annoying source of problems as your project is built and deployed across different environments. Given this API:

```scala
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
fancyLodgingUnsafe(HotelApi())
// res0: Either[Error, Hotel] = Right(
//   value = Hotel(name = "Eddy's Roach Motel")
// )
```

**Collaborator's Machine:**


```scala
fancyLodgingUnsafe(HotelApi())
// res2: Either[Error, Hotel] = Left(
//   value = Error(msg = "Invalid API Key")
// )
```

**Continuous Integration Server:**


```scala
fancyLodgingUnsafe(HotelApi())
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


ZIO has a full `System` implementation of, but we will consider just 1 function for the moment.

```scala
def envZ(
    variable: String
): ZIO[Any, Nothing, Option[String]] =
  ZIO.succeed(sys.env.get("API_KEY"))
```
This merely wraps our original function call.
This is safe, but it is not the easiest code to use or read.
We want to convert an empty `Option` into an error state.

```scala
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

```scala
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


```scala
// TODO This produces large, wide output that does not adhere to the width of the page.
// TODO This has fallen out of sync with the "identical" code below
// TODO Make this a case class?

def fancyLodging(
    hotelApiZ: HotelApiZ
): ZIO[Any, Error, Hotel] =
  hotelApiZ.cheapest("90210")
```

Original, unsafe:

```scala
def fancyLodgingUnsafe(
    hotelApi: HotelApi
): Either[Error, Hotel] =
  hotelApi.cheapest("90210")
// error: 
// missing argument for parameter apiKey of method cheapest in class HotelApi: (zipCode: String, apiKey: String):
//   Either[repl.MdocSession.MdocApp.Error, repl.MdocSession.MdocApp.Hotel]
```

The logic is _identical_ to our original implementation!
The only difference is the result type. 

This is what it looks like in action:

**Your Machine:**


```scala
// TODO Do this for CI environment too
// TODO "originalAuthor" Don't know why it's called that?
val originalAuthor = HotelApiZ.live
```

```scala
val logic =
  defer {
    fancyLodging(ZIO.service[HotelApiZ].run)
  }
// logic: ZIO[HotelApiZ, Nothing, ZIO[Any, Error, Hotel]] = OnSuccess(
//   trace = "zio.direct.ZioMonad.Success.$anon.map(ZioMonad.scala:18)",
//   first = OnSuccess(
//     trace = "repl.MdocSession.MdocApp.<local MdocApp>.logic(13_Environment_Variables.md:233)",
//     first = Sync(
//       trace = "repl.MdocSession.MdocApp.<local MdocApp>.logic(13_Environment_Variables.md:233)",
//       eval = zio.ZIOCompanionVersionSpecific$$Lambda$14263/0x0000000103b74440@5a2e89a7
//     ),
//     successK = zio.ZIO$$$Lambda$14265/0x0000000103b72840@e059359
//   ),
//   successK = zio.ZIO$$Lambda$14274/0x0000000103b8c840@164385a1
// )
runDemo(
  logic.provide(
    SystemStrict.live,
    ZLayer.succeed(HotelApi()),
    originalAuthor
  )
)
```

**Collaborator's Machine:**

```scala
// TODO Do this for CI environment too
val collaborater = HotelApiZ.live

val colaboraterLayer = collaborater
```

```scala
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


```scala
val ci = HotelApiZ.live
```

```scala
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

```scala
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
  runDemoValue(
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
  runDemoValue(
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
  ] = ???
```




## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/13_Environment_Variables.md)
