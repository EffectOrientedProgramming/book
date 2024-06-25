# Managing Failure

Given that Effects encapsulate the unpredictable parts of a system,
they must have a way to express failure.

## Handling Failures


Let's say we have an Effect `getTemperature` which can fail as it tries to make a network request.
First let's run it in the "happy path" assuming it won't fail:

```scala
override val bootstrap =
  happyPath

def run =
  getTemperature
```

Output:

```shell
Result: Temperature(35)
```

As expected, the program succeeds with the temperature.
But if we run it and simulate a network failure, the program will fail.

```scala
override val bootstrap =
  networkFailure

def run =
  getTemperature
```

Output:

```shell
Result: Defect: NetworkException: Network Failure
```

Without any failure handling, the program will not continue past the failed Effect.
For example, if we try to print something after trying to get the temperature,
  an unhandled failure will not allow the program to continue.

```scala
override val bootstrap =
  networkFailure

def run =
  defer:
    getTemperature.run
    Console
      .printLine(
        "only prints if getTemperature succeeds"
      )
      .run
```

Output:

```shell
Result: Defect: NetworkException: Network Failure
```

We can add various forms of failure handling.
One is to "catch" the failure and transform it into another Effect which can also succeed or fail.

```scala
override val bootstrap =
  networkFailure

def run =
  val safeGetTemperature =
    getTemperature.catchAll:
      case e: Exception =>
        ZIO.succeed("Could not get temperature")

  defer:
    safeGetTemperature.run
    Console
      .printLine(
        "will not print if getTemperature fails"
      )
      .run
```

Output:

```shell
will not print if getTemperature fails
```

This time the second Effect will run because we've transformed the `getTemperature` failure into a successful result.

With `catchAll` we must handle all the types of failures and the implementation of `getTemperature` can only fail with an Exception.

We may want to be more specific about how we handle different types of failures.
For example, let's try to catch the `NetworkException`:

```scala
val bad =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"

```

Output:

```shell
[E029] Pattern Match Exhaustivity Warning:
    case ex: NetworkException =>
    ^
match may not be exhaustive.

It would fail on pattern case: _: GpsException
```

This produces a compiler warning because our `catchAll` does not actually catch all possible types of failure.

We should handle all the types of failures:

```scala
val temperatureAppComplete =
  getTemperature.catchAll:
    case ex: NetworkException =>
      ZIO.succeed:
        "Network Unavailable"
    case ex: GpsException =>
      ZIO.succeed:
        "GPS Hardware Failure"
```

Now even if there is a network or GPS failure, the Effect completes successfully:

```scala
override val bootstrap =
  gpsFailure

def run =
  defer:
    val result =
      temperatureAppComplete.run
    Console
      .printLine(
        s"Didn't fail, despite: $result"
      )
      .run
```

Output:

```shell
Didn't fail, despite: GPS Hardware Failure
```

Since the new `temperatureAppComplete` can no longer fail, we can no longer "catch" failures.
Trying to do so will result in a compile error:

```scala
temperatureAppComplete.catchAll:
  case ex: Exception =>
    ZIO.succeed:
      "This cannot happen"
```

Output:

```shell
error: 
This error handling operation assumes your effect
can fail. However, your effect has Nothing for the
error type, which means it cannot fail, so there
is no need to handle the failure.
```

The types of failures from `getTemperature` were both an `Exception`.
But failures can be any type.
For example, we can change the `Exceptions` into another type, like a `String`
  (which is not a good idea, but shows that failures are not constrained to only be `Exception`s)

```scala
val getTemperatureBad =
  getTemperature.catchAll:
    case e: Exception =>
      ZIO.fail:
        e.getMessage
```

Now we can again use `catchAll` but can handle the `String` failure type:

```scala
override val bootstrap =
  gpsFailure

def run =
  getTemperatureBad.catchAll:
    case s: String =>
      Console.printLine(s)
```

Output:

```shell
GPS Failure
```

There are many different ways to handle failures with Effects.
You've already seen some of the others, like `retry` and `orElse` in the **Superpowers** chapter.

With Effects, failures are aggregated across the chain of Effects, unless handled.
For instance, a new `localize` Effect might fail with a new type:

```scala
case class LocalizeFailure(s: String)

def localize(temperature: Temperature) =
  if temperature.degrees > 0 then
    ZIO.succeed("Not too cold.")
  else
    ZIO.fail:
      LocalizeFailure("**Machine froze**")
```

We can now create a new Effect from `getTemperature` and `localize` that can fail with either an `Exception` or a `LocalizeFailure`:

```scala
// can fail with an Exception or a LocalizeFailure
val getTemperatureLocal =
  defer:
    // can fail with an Exception
    val temperature =
      getTemperature.run

    // can fail with a LocalizeFailure
    localize(temperature).run
```

To handle the possible failures for this new Effect, we now need to handle both the `Exception` and `LocalizeFailure`:

```scala
override val bootstrap =
  weird

def run =
  getTemperatureLocal.catchAll:
    case e: Exception =>
      Console.printLine(e.getMessage)
    case LocalizeFailure(s: String) =>
      Console.printLine(s)
```

Output:

```shell
**Machine froze**
```

All the possible failure types, across the sequence of Effects, have been handled at the top-level Effect.
It is up to you when and how you want to handle possible failures.


## Handling Thrown Exceptions

So far we've used Effects to encapsulate failures, but you may have legacy code or external libraries which throw `Exception`s instead.
In these situations there are ways to wrap the `Exception` throwing code and instead have the failures as part of the Effect.


We have an existing `getTemperatureOrThrow` function that can fail by throwing an `Exception`.

If we try to just call this function from an Effect, in the case of failure, the program will fail.

```scala
override val bootstrap =
  networkFailure

def run =
  ZIO.succeed:
    getTemperatureOrThrow()
```

Output:

```shell
Result: Defect: NetworkException: Network Failure
```

Using `ZIO.attempt` we can catch the thrown `Exception`s and instead encapsulate the failure in the Effect.

```scala
val safeTemperatureApp =
  ZIO.attempt:
    getTemperatureOrThrow()
```

Then you can use any of the failure handling mechanisms in Effects to deal with the failure.

```scala
override val bootstrap =
  networkFailure

def run =
  safeTemperatureApp.orElse:
    ZIO.succeed:
      "Could not get temperature"
```

Output:

```shell
Result: Could not get temperature
```

In this case we handle the Effect's failure with a fallback Effect which succeeds.

Since thrown `Exception`s are inherently unpredictable, it is preferable to encapsulate all functions that may throw, into Effects.
This helps make the unpredictability more clear and provides many mechanisms for handling the possible failures. 