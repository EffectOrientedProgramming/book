# Appendix Running Effects

One way to run ZIOs is to use a "main method" program (something you can start in the JVM).
However, setting up the pieces needed for this is a bit cumbersome if done without helpers.

## ZIOAppDefault

ZIO provides an easy way to do this with the `ZIOAppDefault` trait.

To use it create a new `object` that extends the `ZIOAppDefault` trait and implements the `run` method.  That method returns a ZIO so you can now give it the example `ZIO.debug` data:

```scala 3 mdoc
import zio.*
import zio.direct.*

object HelloWorld extends zio.ZIOAppDefault:
  def run =
    ZIO.debug:
      "hello, world"
```

This can be run on the JVM in the same way as any other class that has a `static void main` method.

The `ZIOAppDefault` trait sets up the ZIO runtime which interprets ZIOs and provides some out-of-the-box functionality, and then runs the provided data in that runtime.

If you are learning ZIO, you should start your exploration with `ZIOAppDefault`.
It is the standard, simplest way to start executing your recipes.

For this book we shorten the definition for running ZIO Effects to:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

def run =
  ZIO.debug:
    "hello, world"
```

```scala 3 mdoc
import zio.*
import zio.direct.*
import zio.Console.*

// NOTE We cannot invoke main on this
// because it crashes mdoc in the CI process
object RunningZIOs extends ZIOAppDefault:
  def run =
    printLine:
      "Hello World!"
```

You can provide arbitrary ZIO instances to the run method, as long as you have provided every piece of the environment.
In other words, it can accept `ZIO[Any, _, _]`.

There is a more flexible `ZIOApp` that facilitates sharing layers between applications, but this is advanced and not necessary for most applications.

## ZIOSpecDefault

Similar to `ZIOAppDefault`, there is a `ZIOSpecDefault` that should be your starting point for testing ZIO applications.
`ZIOSpecDefault` provides test-specific implementations built-in services, to make testing easier.
When you run the same `ZIO` in these 2 contexts, the only thing that changes are the built-in services provided by the runtime.

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*
import zio.test.*

object TestingZIOs extends ZIOSpecDefault:
  def spec =
    test("Hello Tests"):
      defer:
        ZIO.debug("** logic **").run
        assertTrue:
          10 > 2
```

For this book we can shorten the test definition to:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, assertTrue}

def spec =
  test("random is random"):
    defer:
      ZIO.debug("** logic **").run
      assertTrue:
        10 > 2
```

## Interop with existing/legacy code

In some cases your ZIOs may need to be run outside a *main* program, for example when embedded into other programs.
In this case you can use ZIO's `Unsafe` utility which is called `Unsafe` to indicate that the code may perform Side Effects.  
To do the same `ZIO.debug` with `Unsafe` do:

```scala 3 mdoc
import zio.*
import zio.direct.*

val out =
  Unsafe.unsafe:
    implicit u: Unsafe =>
      Runtime
        .default
        .unsafe
        .run:
          ZIO.debug:
            "hello, world"
        .getOrThrow()
```

If needed you can even interop to Scala Futures through `Unsafe`, transforming the output of a ZIO into a Future.
