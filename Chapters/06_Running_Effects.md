# Running Effects

## Bruce
You've got a zio that describes something. How do you actually run it?

Although Scala compiles code to JVM bytecodes, ZIO has an interpreter that steps through your code, much like the JVM interprets JVM bytecodes. The Zio interpreter is the hidden piece that allows Zio to understand so much more about the meaning of your code, including the ability to decide what to run concurrently and how to invisibly tune that concurrency--all at runtime. The interpreter is responsible for deciding when to context-switch between tasks, and is able to do this because it understands the ZIO code that it's executing.

The interpreter is also the mechanism that evaluates the various effects described in the generic type parameters for each ZIO object.

The reason we have the `defer` directive in zio-direct is to indicate that this code will be evaluated by the interpreter.

Here's a basic example that shows a ZIO being executed by the interpreter:


## James

If you have a ZIO Effect like:
```scala mdoc
ZIO.debug("hello, world")
```

This doesn't actually do anything.  It only describes something *to be* done.  It is only data (in the ZIO data type), not instructions.  To actually run a ZIO you need to wrap the ZIO in a program that will take the data types and interpret / run them, transforming the descriptions into something that executes.

One way to run ZIOs is to use a "main method" program (something you can start in the JVM) but setting up the pieces needed for this is a bit cumbersome if done without helpers so ZIO provides an easy way to do this with the `ZIOAppDefault` trait.

To use it create a new `object` that extends the `ZIOAppDefault` trait and implements the `run` method.  That method returns a ZIO so you can now give it the example `ZIO.debug` data:
```scala mdoc
object HelloWorld extends zio.ZIOAppDefault:
  override def run =
    ZIO.debug("hello, world")
```

This can be run on the JVM in the same way as any other class that has a `static void main` method.

The `ZIOAppDefault` trait sets up the ZIO runtime which interprets ZIOs and provides some out-of-the-box functionality, and then runs the provided data in that runtime.

In some cases your ZIOs may need to be run outside of a *main* program, for example when embedded into other programs.  In this case you can use ZIO's `Unsafe` utility which is called `Unsafe` to indicate that the code may perform side effects.  To do the same `ZIO.debug` with `Unsafe` do:

```scala mdoc
import zio.Runtime.default.unsafe

Unsafe.unsafe { implicit u: Unsafe =>
  unsafe.run(
    ZIO.debug("hello, world")
  )
  .getOrThrowFiberFailure()
}
```

If needed you can even interop to Scala Futures through `Unsafe`, transforming the output of a ZIO into a Future.





# Bill

A common mistake when starting with ZIO is trying to return ZIO instances themselves rather than their result.
```scala mdoc
println(Random.nextInt)
```
This is a mistake because ZIO's are not their result, they are descriptions of effects that produce the result.
You can think of them as recipes for producing a value.
You don't want to return a recipe from a function, you can only return a value.
If it is your friend's birthday, they want a cake, not a list of instructions about mixing ingredients and baking.


Different ways to run a ZIO:
- As your main application, via `ZIOApp*` variations.
- As a test case
- As the handler for a web request, ala zio-http (Different enough to warrant its own bullet?)
  - Spell out that you only ever execute a top-level ZIO, even if it branches out to multiple other ZIOs
- Within your existing, non-ZIO application, 
  - via `Runtime` & `Unsafe` mechanisms
- Processing a stream of data

## ZIOApp
If you are learning ZIO, you should start your exploration with `ZIOAppDefault`.
It is the standard, simplest way to start executing your recipes.

```scala mdoc
object RunningZIOs extends ZIOAppDefault:
  def run = Console.printLine("Hello World!") 

// RunningZIOs.main(Array.empty)  // causes mdoc crash
```
You can provide arbitrary ZIO instances to the run method, as long as you have provided every piece of the environment.
In other words, it can accept `ZIO[Any, _, _]`.

There is a more flexible `ZIOApp` that facilitates sharing layers between applications, but this is advanced and not necessary for most applications.

## Test
Similar to `ZIOAppDefault`, there is a `ZIOSpecDefault` that should be your starting point for testing ZIO applications.

```scala mdoc
import zio.test._
object TestingZIOs extends ZIOSpecDefault:
    def spec =
      test("Hello Tests"):
        defer:
          assertTrue:
              Random.nextIntBounded(10).run  < 10
```

```scala mdoc
runSpec:
  defer:
    assertTrue:
      Random.nextIntBounded(10).run  < 10
```

```scala mdoc
runSpec:
  Random.nextIntBounded(10)
        .map(x => assertTrue( x < 10))
```

## 
