# ZIO and ZLayer

Connect the DI & Errors chapter to how they are represented in the ZIO data type.

The way we get good compile errors is by having data types which "know" the ...

## ZIO

We need an `Answer` about this scenario.  The scenario requires things and could produce an error.

```scala mdoc:compile-only
trait ZIO[Requirements, Error, Answer]
```

The `ZIO` trait is at the center of our Effect-oriented world.

```scala mdoc:compile-only
trait ZIO[R, E, A]
```

A trait with 3 type parameters can be intimidating, but each one serves a distinct, important purpose.

### R - Requirements

TODO

### E - Errors

This parameter tells us how this operation might fail.

```scala mdoc
def parse(
    contents: String
): ZIO[Any, IllegalArgumentException, Unit] =
  ???
```

### A - Answer

This is what our code will return if it completes successfully.

```scala mdoc
def defaultGreeting()
    : ZIO[Any, Nothing, String] =
  ???
```

## ZLayer

Shares the same type params as ZIO

## Composing

Managing and wiring dependencies has been a perennial challenge in software development.

ZIO provides the `ZLayer` class to solve many of the problems in this space.
If you pay the modest, consistent cost of constructing pieces of your application as `ZLayer`s, you will get benefits that scale with the complexity of your project.
Consistent with `ZIO` itself, `ZLayer` has 3 type parameters that represent:

- What it needs from the environment
- How it can fail
- What it produces when successful.

With the same type parameters, and many of the same methods, you might be wondering why we even need a separate data type - why not just use `ZIO` itself for our dependencies?
The environment type parameter for `ZLayer` maps directly to unique, singleton services in your application.
The environment type parameter for `ZIO` might have _many_ possible instances.
`ZLayer` provides additional behaviors that are valuable specifically in this domain.
Typically, you only want a single instance of a dependency to exist across your application.
This may be to reduce memory/resource usage, or even to ensure basic correctness.
`ZLayer` output values are shared maximally by default.
They also build in scope management that will ensure resource cleanup in asynchronous, fallible situations.

## ZIO <-> ZLayer

`ZLayer.fromZIO`
