# The ZIO Effect & Dependency Data Types

Connect the DI & Errors chapter to how they are represented in the ZIO data type.

The way we get good compile errors is by having data types which "know" the ...


## ZIO Effect Data Type

We need an `Answer` about this scenario.  The scenario requires things and could produce an error.
```
trait ZIO[Requirements, Error, Answer]
```


The `ZIO` trait is at the center of our Effect-oriented world.

```scala
trait ZIO[R, E, A]
```

A trait with 3 type parameters can be intimidating, but each one serves a distinct, important purpose.

## R - The Environment

TODO

## E - The Error

This parameter tells us how this operation might fail.

```scala mdoc
def parse(
    contents: String
): ZIO[Any, IllegalArgumentException, Unit] = ???
```

## A - The Result

This is what our code will return if it completes successfully.

```scala mdoc
def defaultGreeting()
    : ZIO[Any, Nothing, String] = ???
```


## ZIO Dependency Data Type

TODO: ZLayer

