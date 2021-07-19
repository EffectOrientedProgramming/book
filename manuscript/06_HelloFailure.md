# Hello Failures

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
      "30 degress"
```

```scala
def displayTemperature(behavior: String): String =
  "Temperature: " + getTemperature(behavior)
  
displayTemperature("succeed")
// res0: String = "Temperature: 30 degress"
```

On the happy path, everything looks as desired. If the network is unavailable, what is the behavior for the caller? This can take many forms. If we don't make any attempt to handle our problem, the whole program could blow up and show the gory details to the user.

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
  
displayTemperature("Network Error")
// res1: String = "Temperature: null"
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

```scala
// Value.scala

import prep.putStrLn

val basic = putStrLn("hello, world")

```

```scala
// Program.scala
// todo: should fail?
import prep.putStrLn
import zio.Runtime.default

val basic = putStrLn("hello, world")

@main def run = default.unsafeRunSync(basic)
```

```scala
import zio.console.putStrLn
import zio.ZIO

val basic = putStrLn("hello, world")
// basic: ZIO[Console, IOException, Unit] = zio.ZIO$Read@52304cb9

zio.Runtime.default.unsafeRun(basic.catchAll(_ => ZIO.unit))
// hello, world
zio.Runtime.default.unsafeRun(basic)
// hello, world
```
