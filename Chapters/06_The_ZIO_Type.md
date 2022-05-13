# The ZIO Type


We need an `Answer` about this scenario.  The scenario requires things and could produce an error.
```
trait ZIO[Requirements, Error, Answer]
```

One downside of these type parameters 

Functions usually transform the `Answer` from one type to another type.  Errors often aggregate.

```scala mdoc
import zio.ZIO

trait UserService
trait UserNotFound
trait User

trait AccountService
trait AccountError
trait Account

def getUser(userId: String): ZIO[UserService, UserNotFound, User] = ???

def userToAccount(user: User): ZIO[AccountService, AccountError, Account] = ???

def getAccount(userId: String):  ZIO[UserService & AccountService, AccountError | UserNotFound, Account] =
  for
    user <- getUser(userId)
    account <- userToAccount(user)
  yield account
```

```
sealed trait SomeErrors
object AccountError extends SomeErrors
object UserNotFound extends SomeErrors
```

```
case class SomeServices(userService: UserService, accountService: AccountService)

//trait SomeServices extends UserService with AccountService
```

The requirements for each ZIO are combined as an anonymous product type denoted by the `&` symbol.

Scala 3 automatically aggregates the error types by synthesizing an anonymous sum type from the combined errors.

You have the ability to handle all the possible errors from your logic without needing to create a new name that encompasses all of them.

For your `Answer`, it can be desirable to give a clear name that is relevant to your domain.


The `ZIO` trait is at the center of our Effect-oriented world.

```scala
???
```

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
