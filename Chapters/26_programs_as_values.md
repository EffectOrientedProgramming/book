# Programs As Values
        

Abilities:
    - Augment
    - Restrict
    - ???

Each ZIO program is built up from small executable programs that are manipulatible(?) values.
These problems are not yet executing - they are descriptions of code that will be interpreted.
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
def findUser(
    id: String
): ZIO[Any, NotFound, User] =
  ???

def friendsOf(user: User): ZIO[Any, Nothing, List[Friend]] =
  ???

def fullProcess(
    id: String
): ZIO[Any, NotFound, List[Friend]] =
  defer {
    val user = findUser(id).run
    friendsOf(user).run
  }
```
