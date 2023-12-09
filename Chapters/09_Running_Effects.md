# Running Effects

## ZIOs are not their result. 
They are something that can be executed, that _might_ produce that result.
If you have a ZIO Effect like:

```scala mdoc
println("A")
ZIO.debug("B")
println("C")
```

We will not see the `ZIO.debug` output.

It only describes something *to be* done.
It is only data (in the ZIO data type), not instructions.
To actually run a ZIO, your program must take the data types and interpret / run them, executing the logic .

A common mistake when starting with ZIO is trying to return ZIO instances themselves rather than their result.

```scala mdoc
println(Random.nextInt)
```

This is a mistake because ZIO's are not their result, they are descriptions of effects that produce the result.

ZIOs are not automatically executed. 
The user must determine when/where that happens.

An `Option` _might_ have a value inside of it, but you can't safely assume that it does.
Similarly, a `ZIO` _might_ produce a value, but you have to run it to find out.

You can think of them as recipes for producing a value.
You don't want to return a recipe from a function, you can only return a value.
If it is your friend's birthday, they want a cake, not a list of instructions about mixing ingredients and baking.


### The `defer`/direct syntax makes this more explicit


## The ZIO Interpreter

Scala compiles code to JVM bytecodes,
Similarly ZIO has an interpreter that steps through and executes your code, much like the JVM interprets JVM bytecodes.
The Zio interpreter is the hidden piece that allows Zio to understand so much more about the meaning of your code.
This includes the ability to decide what to run concurrently and how to invisibly tune that concurrency--all at runtime.
The interpreter is responsible for deciding when to context-switch between tasks, and is able to do this because it understands the ZIO code that it's executing.

The interpreter is also the mechanism that evaluates the various effects described in the generic type parameters for each ZIO object.

The reason we have the `defer` directive(method?) in zio-direct is to indicate that this code will be evaluated by the interpreter later.

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

```scala mdoc
// NOTE We cannot execute invoke main on this
// because it crashes mdoc in the CI process
object RunningZIOs extends ZIOAppDefault:
  def run =
    //  TODO Console/debug don't work
    ZIO.attempt:
      println:
        "Hello World!"
```


You can provide arbitrary ZIO instances to the run method, as long as you have provided every piece of the environment.
In other words, it can accept `ZIO[Any, _, _]`.

There is a more flexible `ZIOApp` that facilitates sharing layers between applications, but this is advanced and not necessary for most applications.

### runDemo
While the `ZIOApp*` types are great for building real applications, they are not ideal for demonstrating code for a book.
We created the `runDemo` function to streamline this use-case.
It is a function that takes a ZIO and executes it in a runtime, returning the result.
It uses most of the same techniques that are used in `ZIOAppDefault`, but is more single purpose, always immediately executing the ZIO provided to it.

```scala mdoc
runDemo:
  ZIO.debug:
    "hello, world"
```

## Testing code
    - `runSpec` ?

### ZIOSpecDefault

Similar to `ZIOAppDefault`, there is a `ZIOSpecDefault` that should be your starting point for testing ZIO applications.
`ZIOSpecDefault` provides test-specific implementations built-in services, to make testing easier.
When you run the same `ZIO` in these 2 contexts, the only thing that changes are the built-in services provided by the runtime.

> TODO - Decide which scenario to test


```scala mdoc
import zio.test._
object TestingZIOs extends ZIOSpecDefault:
  def spec =
    test("Hello Tests"):
      defer:
        ZIO.console.run
        assertTrue:
          Random.nextIntBounded(10).run > 10
```

### runSpec

```scala mdoc
runSpec:
  defer:
    assertTrue:
      Random.nextIntBounded(10).run < 10
```

TODO Justify defer syntax over for-comp for multi-statement assertions
I think this example completes the objective
TODO Change this to a Console app, where the logic & testing is more visceral

```scala mdoc
runSpec:
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
```scala mdoc
runDemo:
  logic.timeout(1.second)
```
We cannot execute this code and render the results for the book because it requires interaction with a user.
However, even if you are not trying to write demo code for a book, it is very limiting to need a user at the keyboard for your program to execute.
Even for the smallest programs, it is slow, error-prone, and boring.

```scala mdoc
runSpec:
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
Unsafe.unsafe { implicit u: Unsafe =>
  Runtime
    .default
    .unsafe
    .run:
      ZIO.debug:
        "hello, world"
    .getOrThrowFiberFailure()
}
```

If needed you can even interop to Scala Futures through `Unsafe`, transforming the output of a ZIO into a Future.

## Processing streams of data
TODO Consider removing stream content entirely
