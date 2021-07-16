# Hello Failures

There are distinct levels of problems in any given program. They require different types of handling by the programmer.

For instance, you have a program that displays a weather report to the user, retrieved over the network.

```text
Local Temperature: 40 degrees
```

If the network is unavailable, what is the behavior? This can take many forms.

If we don't make any attempt to handle our problem, the whole program could blow up and the gory details could be show
to the user.

```text
NetworkUnavailableException at WeatherRetriever.scala
...
...
```

We could take the bare-minimum approach of catching the `Exception` and returning `null`:

```text
Local Weather: null
```

This is *slightly* better, as the user can at least see the outer structure of our UI element, but it still leaks out
code-specific details world.

Maybe we could fallback to a `sentinel` value, such as `0` or `-1` to indicate a failure?

```text
Local Weather: 0 degrees
Local Weather: -1 degrees
```

Clearly, this isn't acceptable, as both of these common sentinel values are valid temperatures.

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
  override def run(args: List[String]) = basic.catchAll(_ => ZIO.unit).exitCode
```