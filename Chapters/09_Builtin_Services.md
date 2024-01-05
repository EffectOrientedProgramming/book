# Built-in Services

Why are Console, Clock, Random, System Built-in?

`ZIO[Any, _, _]` - System effects are not included in `E`

### Examples of Using System Effects

Some Services are considered fundamental/primitive by ZIO.
They are built-in to the runtime and available to any program.

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
