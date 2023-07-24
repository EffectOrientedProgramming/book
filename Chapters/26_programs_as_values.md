# Programs As Values

Abilities:
    - Augment
    - Restrict
    - ???

Each ZIO program is built up from small executable programs that are manipulatible(?) values.
Because these programs are not executing yet, we can *augment* the original behavior.
This might not sound any better than standard scala functions.

```scala mdoc
trait User
trait Friend
```

```scala mdoc:nest
def findUser(id: String): User = ???
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] = ???
val fullProcess: String => List[Friend] =
  findUser.andThen(friendsOf)
```
That is neat, and often used to get people interested in Functional Programming.
However, if there are more complex types, it quickly becomes less fun

```scala mdoc
trait NotFound
```

```scala mdoc:nest
def findUser(
    id: String
): Either[NotFound, User] = ???
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] = ???
def fullProcess(id: String): Either[NotFound, List[Friend]] =
  findUser(id) match {
    case Right(user) =>
      Right(friendsOf(user))
    case Left(err) =>
      Left(err)
  }
```
TODO Consider: The idiomatic way to do the above is with `flatMap` which we are trying hard to avoid.
               It might not be worth showing this less-happy path, since it would open that can of worms.

```scala mdoc:nest
def findUser(
              id: String
            ): ZIO[Any, NotFound, User] = ???
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] = ???
def fullProcess(id: String): ZIO[Any, NotFound, List[Friend]] =
  defer {
    val user = findUser(id).run
    friendsOf(user)
  }
```
