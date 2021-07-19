# Hello Failures

There are distinct levels of problems in any given program. They require different types of handling by the programmer. Imagine a program that displays the local temperature the user based on GPS position and a network call.

```text
Temperature: 40 degrees
```

```scala mdoc
// helloFailure/noErrorHandling.scala
def getTemperature(): Int =
  30

def displayTemperature(): String =
  "Temperature: " + getTemperature()
  
displayTemperature()

println("second message")

assert(1 == 1)
```

If the network is unavailable, what is the behavior? This can take many forms. If we don't make any attempt to handle our problem, the whole program could blow up and show the gory details to the user.

```scala mdoc:reset
// helloFailure/noErrorHandling.scala
def getTemperature(): Int =
  throw new RuntimeException("Network unavailable!")

def displayTemperature(): String =
  "Temperature: " + getTemperature()
```

```scala mdoc:crash:width=47
// helloFailure/noErrorHandling.scala

displayTemperature()
```


```text
RuntimeException in WeatherRetriever.scala
... rest of stacktrace ...
```

We could take the bare-minimum approach of catching the `Exception` and returning `null`:

```text
Temperature: null
```

This is *slightly* better, as the user can at least see the outer structure of our UI element, but it still leaks out code-specific details world.

Maybe we could fallback to a `sentinel` value, such as `0` or `-1` to indicate a failure?

```text
Temperature: 0 degrees
Temperature: -1 degrees
```

Clearly, this isn't acceptable, as both of these common sentinel values are valid temperatures.
We can take a more honest and accurate approach in this situation.

```text
Temperature: Unavailable
```

We have improved the failure behavior significantly; is it sufficient for all cases?
Imagine our network connection is stable, but we have a problem in our GPS hardware.
In this situation, do we show the same message to the user? Ideally, we would show the user a distinct message for each scenario.
The Network issue is transient, but the GPS problem is likely permanent.

```text
Local Temperature: Network Unavailable
```

```text
Local Temperature: GPS Hardware Failure
```

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

```scala mdoc
import zio.console.putStrLn
import zio.ZIO

val basic = putStrLn("hello, world")

zio.Runtime.default.unsafeRun(basic.catchAll(_ => ZIO.unit))
zio.Runtime.default.unsafeRun(basic)
```
