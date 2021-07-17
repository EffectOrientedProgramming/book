# Hello Failures

There are distinct levels of problems in any given program. They require different types of handling by the programmer.

Imagine a program that displays the local temperature the user based on GPS position and a network call.

```text
Local Temperature: 40 degrees
```

If the network is unavailable, what is the behavior? This can take many forms.

If we don't make any attempt to handle our problem, the whole program could blow up and show the gory details to the
user.

```text
NetworkException in WeatherRetriever.scala
... rest of stacktrace ...
```

We could take the bare-minimum approach of catching the `Exception` and returning `null`:

```text
Local Temperature: null
```

This is *slightly* better, as the user can at least see the outer structure of our UI element, but it still leaks out
code-specific details world.

Maybe we could fallback to a `sentinel` value, such as `0` or `-1` to indicate a failure?

```text
Local Temperature: 0 degrees
Local Temperature: -1 degrees
```

Clearly, this isn't acceptable, as both of these common sentinel values are valid temperatures. We can take a more
honest and accurate approach in this situation.

```text
Local Temperature: Unavailable
```

We have improved the failure behavior significantly; is it sufficient for all cases? Imagine our network connection is
stable, but we have a problem in our GPS hardware. In this situation, do we show the same message to the user? Ideally,
we would show the user a distinct message for each scenario, since the Network issue is transient, but the GPS problem
is likely permanent.

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

```scala
// ZApp.scala

import prep.putStrLn
import zio.{App, ZIO}

val basic = putStrLn("hello, world")

object ZApp extends App:

    override def run(args: List[String]) =
      basic
        .catchAll(_ => ZIO.unit)
        .exitCode

```