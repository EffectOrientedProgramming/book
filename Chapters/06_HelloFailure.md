# Hello Failures

If you are not interested in the discouraged ways to handle errors, and just want to see the ZIO approach, jump down to 
[ZIO Error Handling](#zio-error-handling)

## Historic approaches to Error-handling

There are distinct levels of problems in any given program. They require different types of handling by the programmer. Imagine a program that displays the local temperature the user based on GPS position and a network call.

TODO Show success/failure for all versions

```text
Temperature: 30 degrees
```

```scala mdoc:fmt
class GpsException()     extends RuntimeException
class NetworkException() extends RuntimeException

def getTemperature(behavior: String): String =
  if (behavior == "GPS Error")
    throw new GpsException()
  else if (behavior == "Network Error")
    throw new NetworkException()
  else
    "35 degrees"
```

```scala mdoc
def displayTemperatureUnsafe(
    behavior: String
): String =
  "Temperature: " + getTemperature(behavior)

displayTemperatureUnsafe("succeed")
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
This can take many forms.
If we don't make any attempt to handle our problem, the whole program could blow up and show the gory details to the user.

```scala mdoc:crash
displayTemperatureUnsafe("Network Error")
```

We could take the bare-minimum approach of catching the `Exception` and returning `null`:

```scala mdoc
def displayTemperatureNull(
    behavior: String
): String =
  try
    "Temperature: " + getTemperature(behavior)
  catch
    case (ex: RuntimeException) =>
      "Temperature: " + null

displayTemperatureNull("Network Error")
```

This is *slightly* better, as the user can at least see the outer structure of our UI element, but it still leaks out code-specific details world.

Maybe we could fallback to a `sentinel` value, such as `0` or `-1` to indicate a failure?

```scala mdoc:nest
def displayTemperature(
    behavior: String
): String =
  try
    "Temperature: " + getTemperature(behavior)
  catch
    case (ex: RuntimeException) =>
      "Temperature: -1 degrees"

displayTemperature("Network Error")
```

Clearly, this isn't acceptable, as both of these common sentinel values are valid temperatures.
We can take a more honest and accurate approach in this situation.

```scala mdoc:nest
def displayTemperature(
    behavior: String
): String =
  try
    "Temperature: " + getTemperature(behavior)
  catch
    case (ex: RuntimeException) =>
      "Temperature Unavailable"

displayTemperature("Network Error")
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```scala mdoc:nest
def displayTemperature(
    behavior: String
): String =
  try
    "Temperature: " + getTemperature(behavior)
  catch
    case (ex: NetworkException) =>
      "Network Unavailable"
    case (ex: GpsException) =>
      "GPS problem"

displayTemperature("Network Error")
displayTemperature("GPS Error")
```

Wonderful!
We have specific messages for all relevant error cases. However, this still suffers from downsides that become more painful as the codebase grows.

- The signature of `getTemperature` does not alert us that it might fail
- If we realize it can fail, we must dig through the implementation to discover the multiple failure values

## ZIO Error Handling

Now we will explore how ZIO enables more powerful, uniform error-handling.

TODO Which should we show first?
- [Wrapping Legacy Code](#wrapping-legacy-code)
- [ZIO Error Handling](#zio-error-handling)

### Wrapping Legacy Code
If we are unable to re-write the fallible function, we can still wrap the call

```scala mdoc
import zio.Runtime.default.unsafeRun
import zio.{Task, ZIO}
```

```scala mdoc:fmt
def getTemperatureZWrapped(
    behavior: String
): Task[String] =
  ZIO(getTemperature(behavior)).catchAll {
    case ex: NetworkException =>
      ZIO.succeed("Network Unavailable")
    case ex: GpsException =>
      ZIO.succeed("GPS problem")
  }
```

```scala mdoc
unsafeRun(getTemperatureZWrapped("Succeed"))
```

```scala mdoc
unsafeRun(
  getTemperatureZWrapped("Network Error")
)
```

This is decent, but does not provide the maximum possible guarantees. Look at what happens if we forget to handle one of our errors.

```scala mdoc:fmt
def getTemperatureZGpsGap(
    behavior: String
): ZIO[Any, Exception, String] =
  ZIO(getTemperature(behavior)).catchAll {
    case ex: NetworkException =>
      ZIO.succeed("Network Unavailable")
  }
import mdoc.unsafeRunTruncate
```

```scala mdoc
unsafeRunTruncate(
  getTemperatureZGpsGap("GPS Error")
)
```

The compiler does not catch this bug, and instead fails at runtime. Can we do better?

### ZIO-First Error Handling

```scala mdoc:fmt
// TODO Consult about type param styling
def getTemperatureZ(behavior: String): ZIO[
  Any,
  GpsException | NetworkException,
  String
] =
  if (behavior == "GPS Error")
    ZIO.fail(new GpsException())
  else if (behavior == "Network Error")
    // TODO Use a non-exceptional error
    ZIO.fail(new NetworkException())
  else
    ZIO.succeed("30 degrees")

unsafeRun(getTemperatureZ("Succeed"))
```

```scala mdoc:fail
unsafeRun(
  getTemperatureZ("Succeed").catchAll {
    case ex: NetworkException =>
      ZIO.succeed("Network Unavailable")
  }
)
```

TODO Demonstrate ZIO calculating the error types without an explicit annotation being provided

```scala mdoc:fmt
if 1 == 1 && 2 == 2 && 3 == 3 && 4 == 4 &&
  5 == 5 && 6 == 6
then
  "yay"
else
  "damn"
```