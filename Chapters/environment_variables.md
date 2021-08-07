# Environment Variables

## Historic Approach

```scala mdoc:invisible
// TODO Keep an eye out for a _real_ way to
// provide environment variables to this, while
// keeping my .env separate

// This is a _very_ rough way of imitating a real
// ENV access via `sys.env.get`
case class Env():
  def get(key: String): Option[String] =
    Option
      .when(key == "API_KEY")("SECRET_API_KEY")

case class Sys(env: Env)

val sys = Sys(Env())
```

Environment Variables are a common way of providing dynamic and/or sensitive data to your running application.
A basic use-case looks like this:

```scala mdoc
val apiKey = sys.env.get("API_KEY")
```

This seems rather innocuous; however, it can be an annoying source of problems as your project is built and deployed across different environments.

```scala mdoc:invisible
import scala.util.{Either, Left, Right}

object TravelServiceApi:
  def findCheapestHotel(
      zipCode: String,
      apiKey: String
  ): Either[String, String] =
    if (apiKey == "SECRET_API_KEY")
      Right("Eddy's Roach Motel")
    else
      Left("Invalid API Key")
```

```scala mdoc
def perfectAnniversaryLodging()
    : Either[String, String] =
  val apiKey =
    sys
      .env
      .get("API_KEY")
      .get // Unsafe, but useful for demo
  TravelServiceApi
    .findCheapestHotel("90210", apiKey)

perfectAnniversaryLodging()
```

When you look up an Environment Variable, you are accessing information that was _not_ passed in to your function as an explicit argument.

## Building a Better Way

Before looking at the official ZIO implementation, we will create a simpler version.
We need a `trait` that will indicate what is needed from the environment.

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

Finally, for easier usage by the caller, we create an accessor in the companion object.

```scala mdoc
import zio.Has

object System:
  def env(
      variable: => String
  ): ZIO[Has[System], Nothing, Option[String]] =
    ZIO.accessZIO(_.get.env(variable))
```

Now if we use this code, our caller's type signature is forced to tell us that it requires a `System` to execute.

```scala mdoc
def perfectAnniversaryLodgingSafe(): ZIO[Has[
  System
], Nothing, Either[String, String]] =
  for
    apiKey <- System.env("API_KEY")
  yield TravelServiceApi.findCheapestHotel(
    "90210",
    apiKey.get // unsafe!
  )
```

This is what it looks like in action:

```scala mdoc
import zio.Runtime.default.unsafeRun
import zio.ZLayer

unsafeRun(
  perfectAnniversaryLodgingSafe().provideLayer(
    ZLayer.succeed[System](SystemLive())
  )
)
```

When constructed this way, it becomes very easy to test.
We create a second implementation that accepts test values and serves them to the caller.

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
  perfectAnniversaryLodgingSafe().provideLayer(
    ZLayer.succeed[System](
      SystemHardcoded(
        Map("API_KEY" -> "Invalid Key")
      )
    )
  )
)
```


## Official ZIO Approach

TODO

```scala mdoc
import zio.System

def perfectAnniversaryLodgingZ(): ZIO[Has[
  System
], Nothing, Either[String, String]] =
  for
    apiKey <- System.env("API_KEY")
  yield TravelServiceApi.findCheapestHotel(
    "90210",
    apiKey.get // unsafe!
  )
```