# Environment Variables

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
object TravelServiceApi:
  def findCheapestHotel(
      zipCode: String,
      apiKey: String
  ): Option[String] =
    Option.when(apiKey == "SECRET_API_KEY")(
      "Bargain Eddy's Roach Motel"
    )
```

```scala mdoc
def findPerfectAnniversaryLodging() =
  val apiKey =
    sys
      .env
      .get("API_KEY")
      .getOrElse(
        throw new RuntimeException("boom")
      )
  TravelServiceApi
    .findCheapestHotel("90210", apiKey)

findPerfectAnniversaryLodging()
```

When you look up an Environment Variable, you are accessing information that was _not_ passed in to your function as an explicit argument.

