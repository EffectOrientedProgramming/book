# Console

## The Unprincipled Way

This is generally the first effect that we will want as we learn to construct functional programs.
It is so basic that most languages do not consider it as anything special.
The typical first scala program is something like:

```scala mdoc
println("Hi there.")
```

Simple enough, and familiar to anyone that has programmed before.
Take a look at the signature of this function in the Scala `Predef` object:

```scala mdoc:nest
def println(x: Any): Unit = ???
```

Based on the name, it is likely that the `Console` is involved.
Unfortunately the type signature does not indicate that.
If we check the implementation, we discover this:

```scala mdoc:nest
def println(x: Any): Unit = Console.println(x)
```

Now it is clear that we are printing to the `Console`.
If we do not have access to the implementation source code, this is a surprise to us at runtime.

## Building a Better Way

Before looking at the official ZIO implementation, we will create a simpler version.

TODO: Decide whether explaining this pattern belongs in a standalone section.
      It is important in isolation, but probably hard to appreciate without a use-case, and `Console` is likely the simplest example.

The pattern used here is fundamental to designing composable, ergonomic ZIO `Services`.

1. Create a `trait` with the needed functions.
2. Create an implementation of the `trait`.
3. (Optional) Put "accessor" methods in `trait` companion object.
4. (Optional) Provide implementation instance in a `Layer` as a `object` field - `live`.

We will go through each of these steps in detail in this chapter, and more concisely in the rest.
Steps 1 and 2 steps will be familiar to many programmers.
Steps 3 and 4 are less familiar, and a bit harder to appreciate.
We endeavor in the following chapters to make a compelling case for them.
If we succeed, the reader will add them when creating their own Effects.

### One: Create the trait

This `trait` represents a piece of the `Environment` that our codes need to interact with.
It contains the methods for effectful interactions.

```scala mdoc
import zio.ZIO

trait Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit]
```

### Two: Create the implementation

```scala mdoc
object ConsoleLive extends Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit] =
    // TODO Get this working without Predef
    ZIO.succeed(Predef.println(output))
```

TODO{Determine how to best split the 2 pieces we need to add to the same `object` for these steps}

### Three: Create Accessor Methods in Companion

The first two steps are enough for us to track Effects in our system, but the ergonomics are not great.

```scala mdoc:nest
val logicClunky: ZIO[Console, Nothing, Unit] =
  for
    _ <-
      ZIO.serviceWithZIO[Console](
        _.printLine("Hello")
      )
    _ <-
      ZIO.serviceWithZIO[Console](
        _.printLine("World")
      )
  yield ()

import zio.Runtime.default.unsafeRun
import zio.ZLayer
unsafeRun(
  logicClunky.provide(
    ZLayer.succeed[Console](ConsoleLive)
  )
)
```

The caller has to handle the ZIO environment access, which is a distraction from the logic they want to implement.

```scala mdoc
// TODO Consider deleting this entirely

// TODO remove alt companions and make top-level
// functions
object ConsoleWithAccessor:
  def printLine(
      variable: => String
  ): ZIO[Console, Nothing, Unit] =
    ZIO.serviceWith(_.printLine(variable))
```

With this function, our callers have a much nicer experience.

```scala mdoc
val logic: ZIO[Console, Nothing, Unit] =
  for
    _ <- ConsoleWithAccessor.printLine("Hello")
    _ <- ConsoleWithAccessor.printLine("World")
  yield ()
```

However, providing dependencies to the logic is still tedious.

```scala mdoc
import zio.ZLayer
import zio.Runtime.default.unsafeRun
unsafeRun(
  logic.provide(
    ZLayer.succeed[Console](ConsoleLive)
  )
)
```

### Four: Create `object Effect.live` field

Rather than making each caller wrap our instance in a `Layer`, we can do that a single time in our companion.

```scala mdoc
import zio.ZLayer

object ConsoleWithLayer:
  val live: ZLayer[Any, Nothing, Console] =
    ZLayer.succeed[Console](ConsoleLive)
```

Now executing our code is as simple as describing it.

```scala mdoc
unsafeRun(logic.provide(ConsoleWithLayer.live))
```

In real application, both of these will go in the companion object directly.

```scala mdoc
import zio.ZLayer
object Console:
  def printLine(
      variable: => String
  ): ZIO[Console, Nothing, Unit] =
    ZIO.serviceWith(_.printLine(variable))

  val live: ZLayer[Any, Nothing, Console] =
    ZLayer.succeed[Console](ConsoleLive)
```

## Official ZIO Approach

TODO

## ZIO Super-Powers

```scala mdoc
object ConsoleSanitized extends Console:
  private val socialSecurity =
    "\\d{3}-\\d{2}-\\d{4}"

  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit] =
    val sanitized =
      output
        .replaceAll(
          socialSecurity,
          "***-**-****"
        )
        .nn
    // TODO ugh. String methods with explicit
    // nulls *suck*
    ConsoleLive.printLine(sanitized)
```

```scala mdoc:silent
val leakSensitiveInfo
    : ZIO[Console, Nothing, Unit] =
  Console
    .printLine("Customer SSN is 000-00-0000")
```

```scala mdoc
unsafeRun(
  leakSensitiveInfo.provide(
    ZLayer.succeed[Console](ConsoleSanitized)
  )
)
```
