# Console
NON-MDOC examples throughout this file after 2.0.0 upgrade. TODO Fix before release

## The Unprincipled Way

This is generally the first effect that we will want as we learn to construct functional programs.
It is so basic that most languages do not consider it as anything special.
The typical first scala program is something like:

```scala
Predef.println("Hi there.")
// Hi there.
```

Simple enough, and familiar to anyone that has programmed before.
Take a look at the signature of this function in the Scala `Predef` object:

```scala
def println(x: Any): Unit = ???
```

Based on the name, it is likely that the `Console` is involved.
Unfortunately the type signature does not indicate that.
If we do not have access to the implementation source code, this is a surprise to us at runtime.

## Building a Better Way

Before looking at the official ZIO implementation, we will create a simpler version.

TODO: Decide whether explaining this pattern belongs in a standalone section.
      It is important in isolation, but probably hard to appreciate without a use-case, and `Console` is likely the simplest example.

The pattern used here is fundamental to designing composable, ergonomic ZIO `Services`.

1. Create a `trait` with the needed functions.
2. Create an implementation of the `trait`.
3. (Optional) Provide implementation instance in a `Layer` as a `object` field - `live`.

We will go through each of these steps in detail in this chapter, and more concisely in the rest.
Steps 1 and 2 steps will be familiar to many programmers.
Steps 3 is less familiar, and might be harder to appreciate.
We endeavor in the following chapters to make a compelling case for them.
If we succeed, the reader will use them when creating their own Effects.

### One: Create the trait

This `trait` represents effectful code that we need to interact with.

```scala
trait Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit]
```

### Two: Create the implementation

```scala
object ConsoleLive extends Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit] =
    // TODO Get this working without Predef
    ZIO.succeed(Predef.println(output))
```

```scala
case class Logic(console: Console):
  val invoke: ZIO[Any, Nothing, Unit] =
    defer {
      console.printLine("Hello").run
      console.printLine("World").run
    }
```

However, providing dependencies to the logic is still tedious.

```scala
runDemo(Logic(ConsoleLive).invoke)
// Hello
// World
// ()
```

### Three: Create `object Effect.live` field

Rather than making each caller wrap our instance in a `Layer`, we can do that a single time in our companion.

```scala
object Console:
  val live: ZLayer[Any, Nothing, Console] =
    ZLayer.succeed[Console](ConsoleLive)
```
More important than removing repetition - using 1 unique Layer instance per type allows us to share it across our application.

Now executing our code is as simple as describing it.

```scala
runDemo(
  ZIO
    .serviceWithZIO[Logic](_.invoke)
    .provide(
      Console.live,
      ZLayer.fromFunction(Logic.apply _)
    )
)
// Hello
// World
// ()
```


## Official ZIO Approach

TODO

## ZIO Super-Powers

#### Single expression debugging
When debugging code, we often want to stick a `println` among our logic.

```scala
def crunch(a: Int, b: Int) = (a * 2) / (a * 10)
```
Historically, this has caused friction for chained expressions.
We must surround our expression in braces, in order to add this _statement_ before it.
TODO Disclaimer that this is less compelling in a "fewer braces" world

```scala
def crunchDebugged(a: Int, b: Int) =
  Predef.println("")
  a * a
```


```scala
runDemo(
  ZIO.debug("ping") *>
    ConsoleLive.printLine("Normal logic")
)
// ping
// Normal logic
// ()
```

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

```scala
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
// Customer SSN is 000-00-0000
// ()
```


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/console/FakeConsole.scala
```scala
package console

import zio.Console
import zio.Console.*

import java.io.IOException

object FakeConsole:

  val name: Console = single("(default name)")

  val word: Console   = single("Banana")
  val number: Console = single("1")

  def single(hardcodedInput: String) =
    new Console:
      def print(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(
          scala
            .Console
            .print("Hard-coded: " + line)
        )
      def printError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???
      def printLine(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(
          println("Hard-coded: " + line)
        )
      def printLineError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???
      def readLine(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, String] =
        ZIO.succeed(hardcodedInput)

  def withInput(
      hardcodedInput: String*
  ): ZIO[Any, Nothing, Console] =
    defer {
      val inputVariable =
        Ref.make(hardcodedInput.toSeq).run
      inputConsole(inputVariable)
    }

  private def inputConsole(
      hardcodedInput: Ref[Seq[String]]
  ) =
    new Console:
      def print(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(scala.Console.print(line))

      def printError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???

      def printLine(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO
          .succeed(println("Automated: " + line))

      def printLineError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???

      def readLine(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, String] =
        defer {
          val curInput = hardcodedInput.get.run
          hardcodedInput.set(curInput.tail).run
          curInput.head
        }

end FakeConsole

```

