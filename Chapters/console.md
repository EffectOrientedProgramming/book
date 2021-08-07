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
4. (Optional) Wrap an instance the implementation class in a `Layer` and provide as a field - `live` in the companion `object`.

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

### Three: Create Accessor Methods in Companion

## Official ZIO Approach
