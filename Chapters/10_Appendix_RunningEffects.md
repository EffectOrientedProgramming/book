# Appendix Running Effects

TODO: Make an appendix?

## Building applications from scratch

One way to run ZIOs is to use a "main method" program (something you can start in the JVM).
However, setting up the pieces needed for this is a bit cumbersome if done without helpers.

### ZIOAppDefault

ZIO provides an easy way to do this with the `ZIOAppDefault` trait.

To use it create a new `object` that extends the `ZIOAppDefault` trait and implements the `run` method.  That method returns a ZIO so you can now give it the example `ZIO.debug` data:

```scala mdoc
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

```scala mdoc:runzio
def run =
  ZIO.debug:
    "hello, world"
```

```scala mdoc
// NOTE We cannot execute invoke main on this
// because it crashes mdoc in the CI process
object RunningZIOs extends ZIOAppDefault:
  def run =
    Console.printLine:
      "Hello World!"
```

You can provide arbitrary ZIO instances to the run method, as long as you have provided every piece of the environment.
In other words, it can accept `ZIO[Any, _, _]`.

There is a more flexible `ZIOApp` that facilitates sharing layers between applications, but this is advanced and not necessary for most applications.

## Testing code

### ZIOSpecDefault

Similar to `ZIOAppDefault`, there is a `ZIOSpecDefault` that should be your starting point for testing ZIO applications.
`ZIOSpecDefault` provides test-specific implementations built-in services, to make testing easier.
When you run the same `ZIO` in these 2 contexts, the only thing that changes are the built-in services provided by the runtime.

> TODO - Decide which scenario to test

```scala mdoc:compile-only
object TestingZIOs extends ZIOSpecDefault:
  def spec =
    test("Hello Tests"):
      defer:
        ZIO.console.run
        assertTrue:
          Random.nextIntBounded(10).run > 10
```

For this book we can shorten the test definition to:

```scala mdoc:testzio
def spec =
  test("random is random"):
    defer:
      assertTrue:
        Random.nextIntBounded(10).run < 10
```

TODO Justify defer syntax over for-comp for multi-statement assertions
I think this example completes the objective
TODO Change this to a Console app, where the logic & testing is more visceral

```scala mdoc:testzio
def spec =
  test("random is still random"):
    defer:
      assertTrue:
        Random.nextIntBetween(0, 10).run <= 10 &&
        Random.nextIntBetween(10, 20).run <=
          20 &&
          Random.nextIntBetween(20, 30).run <= 30
```

Consider a `Console` application:

```scala mdoc:silent
val logic =
  defer:
    val username =
      Console
        .readLine:
          "Enter your name\n"
        .run
    Console
      .printLine:
        s"Hello $username"
      .run
  .orDie
```

If we try to run this code in the same way as most of the examples in this book, we encounter a problem.

```scala mdoc:compile-only
object HelloWorldWithTimeout
    extends zio.ZIOAppDefault:
  def run =
    logic.timeout(1.second)
```

We cannot execute this code and render the results for the book because it requires interaction with a user.
However, even if you are not trying to write demo code for a book, it is very limiting to need a user at the keyboard for your program to execute.
Even for the smallest programs, it is slow, error-prone, and boring.

```scala mdoc:testzio
def spec =
  test("console works"):
    defer:
      TestConsole
        .feedLines:
          "Zeb"
        .run

      logic.run

      val capturedOutput: String =
        TestConsole.output.run.mkString
      val expectedOutput =
        s"""|Enter your name
            |Hello Zeb
            |""".stripMargin
      assertTrue:
        capturedOutput == expectedOutput
```

## Interop with existing/legacy code via Unsafe

In some cases your ZIOs may need to be run outside a *main* program, for example when embedded into other programs.
In this case you can use ZIO's `Unsafe` utility which is called `Unsafe` to indicate that the code may perform side effects.  
To do the same `ZIO.debug` with `Unsafe` do:

```scala mdoc
val out =
  Unsafe.unsafe:
    implicit u: Unsafe =>
      Runtime
        .default
        .unsafe
        .run:
          ZIO.debug:
            "hello, world"
        .getOrThrowFiberFailure()
```

If needed you can even interop to Scala Futures through `Unsafe`, transforming the output of a ZIO into a Future.
