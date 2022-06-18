# Mutability

Functional programmers often sing the praises of immutability.
The advantages are real and numerous.
However, it is easy to find situations that are intrinsically mutable.

- How many people are currently inside a building?
- How much fuel is in your car?
- How much money is in your bank account?
- TODO more

*TODO Consider deleting*
It is true that many of these concepts can be derived from a sequence of state transformations.
For example, the number of people in a building can be calculated from the number of people who have entered the building and the number of people who have left the building.

```
Seq(
  Entered(2),
  Exited(1),
  Entered(3),
  Exited(2),
)
```
However, this can be tedious to work with.
We want a way to jump straight to the current state of the system.
*/TODO*
    

Rather than avoiding mutability entirely, we want to avoid unprincipled, unsafe mutability.
If we codify and enumerate everything that we need from Mutability, then we can wield it safely.
Required Operations:

- Change the value
- Read the current value

These are both effectful operations.

```scala mdoc
import zio.UIO

trait RefZ[A]:
  def get: UIO[A]
  def set(a: A): UIO[Unit]
```

Less obviously, we also need to create the Mutable reference itself.
We are changing the world, by creating a space that we can manipulate.
This operation can live in the companion object:

```scala mdoc
object RefZ:
  def make[A](a: A): UIO[RefZ[A]] = ???
```

In order to confidently use this, we need certain guarantees about the behavior:

- The underlying value cannot be changed during a read
- Multiple writes cannot happen concurrently, which would result in lost updates

#### Unreliable Counting

```scala mdoc
import zio.{Ref, ZIO}
import mdoc.unsafeRunPrettyPrint

var counter = 0
def increment() =
  ZIO.succeed {
    counter = counter + 1
    counter
  }

val unreliableCounting =
  for _ <-
      ZIO.foreachParDiscard(Range(0, 10000))(
        _ => increment()
      )
  yield "Final count: " + counter

unsafeRunPrettyPrint(unreliableCounting)
```

Performing our side effects inside ZIO's does not magically make them safe.
We need to fully embrace the ZIO components, utilizing `Ref` for correct mutation.

#### Reliable Counting

```scala mdoc

def incrementCounter(counter: Ref[Int]) =
  counter.update(_ + 1)

val reliableCounting =
  for
    counter <- Ref.make(0)
    _ <-
      ZIO
        .foreachParDiscard(Range(0, 10000))(
          _ => incrementCounter(counter)
        )
    finalResult <- counter.get
  yield "Final count: " + finalResult
  
unsafeRunPrettyPrint(reliableCounting)
```