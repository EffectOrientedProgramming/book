# The ZIO Type
The `ZIO` trait is at the center of our Effect-oriented world.
```scala
trait ZIO[R, E, A]
```

```scala mdoc
import zio.ZIO
```
A trait with 3 type parameters can be intimidating, but each one serves a distinct, important purpose.

## R - The Environment
This is the piece that distinguishes the ZIO monad.
It indicates which pieces of the world we will be observing or changing.

```scala mdoc
import zio.Console

def print(
    msg: String
): ZIO[Console, Nothing, Unit] = ???
```
This type signature tells us that `print` needs a `Console` in its environment to execute.


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
