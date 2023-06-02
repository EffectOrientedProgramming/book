# The ZIO Type


We need an `Answer` about this scenario.  The scenario requires things and could produce an error.
```
trait ZIO[Requirements, Error, Answer]
```

One downside of these type parameters 


The `ZIO` trait is at the center of our Effect-oriented world.

```scala
trait ZIO[R, E, A]
```

```scala
import zio.ZIO
```

A trait with 3 type parameters can be intimidating, but each one serves a distinct, important purpose.

## R - The Environment

This is the piece that distinguishes the ZIO monad.
It indicates which pieces of the world we will be observing or changing.

```scala
import zio.Console

def print(
    msg: String
): ZIO[Console, Nothing, Unit] = ???
```

This type signature tells us that `print` needs a `Console` in its environment to execute.

## E - The Error

This parameter tells us how this operation might fail.

```scala
def parse(
    contents: String
): ZIO[Any, IllegalArgumentException, Unit] = ???
```

## A - The Result

This is what our code will return if it completes successfully.

```scala
def defaultGreeting()
    : ZIO[Any, Nothing, String] = ???
```

## Conversions from standard Scala types
ZIO provides simple interop with may of the built-in Scala data types, namely

- `Option`
- `Either`
- `Try`
- `scala.concurrent.Future`
- `Promise`

And even some Java types -

- `java.util.concurrent.Future`
- `AutoCloseable`

```scala
import zio.{ZIO, ZIOAppDefault}
import scala.concurrent.Future
import mdoc.unsafeRunPrettyPrint
val zFuture =
  ZIO.fromFuture(implicit ec =>
    Future.successful("Success!")
  )
// zFuture: ZIO[Any, Throwable, String] = Stateful(
//   trace = "repl.MdocSession.MdocApp.zFuture(06_The_ZIO_Type.md:47)",
//   onState = zio.ZIO$$$Lambda$2475/0x0000000100ba2040@71b3c2a7
// )
val zFutureFailed =
  ZIO.fromFuture(implicit ec =>
    Future.failed(new Exception("Failure :("))
  )
// zFutureFailed: ZIO[Any, Throwable, Nothing] = Stateful(
//   trace = "repl.MdocSession.MdocApp.zFutureFailed(06_The_ZIO_Type.md:54)",
//   onState = zio.ZIO$$$Lambda$2475/0x0000000100ba2040@11869005
// )
unsafeRunPrettyPrint(zFuture)
// Success!
unsafeRunPrettyPrint(zFutureFailed)
// java.lang.Exception: Failure :(
```