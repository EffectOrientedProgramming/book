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

```scala mdoc:nest
trait NotFound
def findUser(
    id: String
): Either[NotFound, User] = ???
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] = ???
val fullProcess
    : String => Either[NotFound, List[Friend]] =
  findUser.andThen {
    case Right(user) =>
      Right(friendsOf(user))
    case error: Left =>
      error
  }
```
