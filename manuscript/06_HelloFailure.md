# Hello Failures

If you are not interested in the discouraged ways to handle errors, and just want to see the ZIO approach, jump down to 
[ZIO Error Handling](#zio-error-handling)

## Historic approaches to Error-handling

There are distinct levels of problems in any given program. They require different types of handling by the programmer. Imagine a program that displays the local temperature the user based on GPS position and a network call.

```text
Temperature: 30 degrees
```

```scala
class GpsException() extends RuntimeException
class NetworkException() extends RuntimeException

def getTemperature(behavior: String): String =
    if (behavior == "GPS Error")
      throw new GpsException()
    else if (behavior == "Network Error")
      throw new NetworkException()
    else
      "35 degress"
```

```scala
def displayTemperature(behavior: String): String =
  "Temperature: " + getTemperature(behavior)
  
displayTemperature("succeed")
// res0: String = "Temperature: 35 degress"
```

On the happy path, everything looks as desired.
If the network is unavailable, what is the behavior for the caller?
This can take many forms.
If we don't make any attempt to handle our problem, the whole program could blow up and show the gory details to the user.

```scala
def displayTemperature(behavior: String): String =
    "Temperature: " + getTemperature(behavior)

displayTemperature("Network Error")
// repl.MdocSession$App$NetworkException
// 	at repl.MdocSession$App.getTemperature(06_HelloFailure.md:17)
// 	at repl.MdocSession$App.displayTemperature$1(06_HelloFailure.md:26)
// 	at repl.MdocSession$App.$init$$$anonfun$2(06_HelloFailure.md:44)
```

We could take the bare-minimum approach of catching the `Exception` and returning `null`:

```scala
def displayTemperature(behavior: String): String =
  val temperature =
    try
      getTemperature(behavior)
    catch
      case (ex: RuntimeException) => null
    
  "Temperature: " + temperature
  
assert( 
  displayTemperature("Network Error") == 
  "Temperature: null"
)
```

This is *slightly* better, as the user can at least see the outer structure of our UI element, but it still leaks out code-specific details world.

Maybe we could fallback to a `sentinel` value, such as `0` or `-1` to indicate a failure?

```scala
def displayTemperature(behavior: String): String =
  val temperature =
    try
      getTemperature(behavior)
    catch
      case (ex: RuntimeException) => "-1 degrees"
    
  "Temperature: " + temperature
  
displayTemperature("Network Error")
// res2: String = "Temperature: -1 degrees"
```

Clearly, this isn't acceptable, as both of these common sentinel values are valid temperatures.
We can take a more honest and accurate approach in this situation.

```scala
def displayTemperature(behavior: String): String =
  val temperature =
    try
      getTemperature(behavior)
    catch
      case (ex: RuntimeException) => "Unavailable"
    
  "Temperature: " + temperature
  
displayTemperature("Network Error")
// res3: String = "Temperature: Unavailable"
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```scala
def displayTemperature(behavior: String): String =
  val temperature =
    try
      getTemperature(behavior)
    catch
      case (ex: NetworkException) => "Network Unavailable"
      case (ex: GpsException) => "GPS problem"
    
  "Temperature: " + temperature
  
displayTemperature("Network Error")
// res4: String = "Temperature: Network Unavailable"
displayTemperature("GPS Error")
// res5: String = "Temperature: GPS problem"
```

Wonderful!
We have specific messages for all relevant error cases. However, this still suffers from downsides that become more painful as the codebase grows.

- The signature of `getTemperature` does not alert us that it might fail
- If we realize it can fail, we must dig through the implementation to discover the multiple failure values


## ZIO Error Handling

Now we will explore how ZIO enables more powerful, uniform error-handling.
If we are unable to re-write the fallible function, we can still improve 

```scala
import zio.Runtime.default.unsafeRun
import zio.{Task, ZIO}
```

```scala
def getTemperatureZWrapped(behavior: String): Task[String] =
    ZIO(getTemperature(behavior))
        .catchAll{
          case (ex: NetworkException) => ZIO.succeed("Network Unavailable")
          case (ex: GpsException) => ZIO.succeed("GPS problem")
        }
```

```scala
unsafeRun(
  getTemperatureZWrapped("Succeed")
)
// res6: String = "35 degress"
```

```scala
unsafeRun(
  getTemperatureZWrapped("Network Error")
)
// res7: String = "Network Unavailable"
```

```scala
def getTemperatureZ(behavior: String) =
    if (behavior == "GPS Error")
      ZIO.fail(new GpsException())
    else if (behavior == "Network Error")
      ZIO.fail(new NetworkException())
    else
      ZIO.succeed("30 degrees")

unsafeRun(
  getTemperatureZ("Succeed")
)
// res8: String = "30 degrees"

getTemperatureZ("Succeed")
// res9: ZIO[Any, GpsException | NetworkException, String] = zio.ZIO$EffectTotal@129cf220
```

Even though we did not provide an explicit result type for this function, ZIO & Scala are smart enough to construct it