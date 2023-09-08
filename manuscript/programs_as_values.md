# Programs As Values
        

Abilities:
    - Augment
    - Restrict
    - ???

Each ZIO program is built up from small executable programs that are manipulatible(?) values.
These problems are not yet executing - they are descriptions of code that will be interpreted.
Because these programs are not executing yet, we can *augment* the original behavior.
This might not sound any better than standard scala functions.

```scala
trait User
trait Friend
```

```scala
def findUser(id: String): User = ???
// You can augment findUser by using `.andThen` to attach new behavior

def friendsOf(user: User): List[Friend] = ???
val fullProcess: String => List[Friend] =
  findUser.andThen(friendsOf)
// error:
// unused local definition
//     id: String
//     ^^
// error:
// unused local definition
//     user: User
//     ^^^^
// error: 
// unused local definition
```
That is neat, and often used to get people interested in Functional Programming.
However, if there are more complex types, it quickly becomes less fun

```scala
case class NotFound()
```



```scala
def findUser(
    id: String
): ZIO[Any, NotFound, User] = ???

def friendsOf(
    user: User
): ZIO[Any, Nothing, List[Friend]] = ???

def fullProcess(
    id: String
): ZIO[Any, NotFound, List[Friend]] =
  defer {
    val user = findUser(id).run
    friendsOf(user).run
  }
// error:
// unused local definition
//     id: String
//     ^^
// error:
// unused local definition
//     user: User
//     ^^^^
// error:
// unused local definition
// def fullProcess(
//     ^^^^^^^^^^^
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/programs_as_values.md)
