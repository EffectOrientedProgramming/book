# Built-in Services

## System Effects

Why are Console, Clock, Random, System Built-in?

`ZIO[Any, _, _]` - System effects are not included in `E`

### Examples of Using System Effects

Some Services are considered fundamental/primitive by ZIO.
They are built-in to the runtime and available to any program.

## History - TODO Consider deleting. Not crucial to the reader.

### 1.x
Originally, ZIO merely provided default implementations of these services.
There was nothing else special about them.
If you wanted to write to the console in your code, you needed a `ZIO[Console, _, _].
If you wanted to write random, timestamped numbers, accompanied by some system information to the console,
you needed a `ZIO[Random with Clock with System with Console, _, _].
This is maximally informative, but it is also a lot of boilerplate code.

### 2.x
Late in the development of ZIO 2.0, the team decided to bake these deeper into the runtime.
Now you can use any of these services without an impact on your method signatures.
This reduces boilerplate, with a trade-off.
You can no longer discern _which_ piece of the Environment/Runtime is being accessed by reading the signature.

## Overriding Builtin Services

> Note: This doesn't work in ZIO 2

```scala
object ConsoleSanitized extends Console:
  private val socialSecurity =
    "\\d{3}-\\d{2}-\\d{4}"

  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit] =
    val sanitized =
      output.replaceAll(
        socialSecurity,
        "***-**-****"
      )
    ConsoleLive.printLine(sanitized)
```

```scala mdoc:silent
val leakSensitiveInfo
    : ZIO[Console, java.io.IOException, Unit] =
  zio
    .Console
    .printLine("Customer SSN is 000-00-0000")
```

```scala
runDemo(
  leakSensitiveInfo.provide(
    ZLayer.succeed[Console](ConsoleSanitized)
  )
)
```
