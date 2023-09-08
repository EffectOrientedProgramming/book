# Running Effects

## Bruce
You've got a zio that describes something. How do you actually run it?

Although Scala compiles code to JVM bytecodes, ZIO has an interpreter that steps through your code, much like the JVM interprets JVM bytecodes. The Zio interpreter is the hidden piece that allows Zio to understand so much more about the meaning of your code, including the ability to decide what to run concurrently and how to invisibly tune that concurrency--all at runtime. The interpreter is responsible for deciding when to context-switch between tasks, and is able to do this because it understands the ZIO code that it's executing.

The interpreter is also the mechanism that evaluates the various effects described in the generic type parameters for each ZIO object.

The reason we have the delay directive in zio-direct is to indicate that this code will be evaluated by the interpreter.

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