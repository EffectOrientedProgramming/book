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

```scala mdoc:nest:fail
def findUser(id: String): User = ???
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] = ???
val fullProcess: String => List[Friend] =
  findUser.andThen(friendsOf)
```
That is neat, and often used to get people interested in Functional Programming.
However, if there are more complex types, it quickly becomes less fun

```scala mdoc
case class NotFound()
```

```scala mdoc:invisible
// Fixes "unused local definition"
// since later examples fail
println(NotFound())
```

```scala mdoc:nest:fail
// Have to use `id` or else we get a compiler error
// TODO Configure build/mdoc to allow unused parameters sometimes.
def findUser(
    id: String
): Either[NotFound, User] =
  throw new NotImplementedError(id)
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] =
  throw new NotImplementedError(user.toString)
def fullProcess(
    id: String
): Either[NotFound, List[Friend]] =
  findUser(id) match
    case Right(user) =>
      Right(friendsOf(user))
    case Left(err) =>
      Left(err)
```
TODO Consider: The idiomatic way to do the above is with `flatMap` which we are trying hard to avoid.
               It might not be worth showing this less-happy path, since it would open that can of worms.

```scala mdoc:nest:fail
def findUser(
    id: String
): ZIO[Any, NotFound, User] =
  throw new NotImplementedError(id)
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] =
  throw new NotImplementedError(user.toString)

def fullProcess(
    id: String
): ZIO[Any, NotFound, List[Friend]] =
  defer {
    val user = findUser(id).run
    friendsOf(user)
  }
```
