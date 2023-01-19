# Mutability

Functional programmers often sing the praises of immutability.
The advantages are real and numerous.
However, it is easy to find situations that are intrinsically mutable.

- How many people are currently inside a building?
- How much fuel is in your car?
- How much money is in your bank account?

Rather than avoiding mutability entirely, we want to avoid unprincipled, unsafe mutability.
If we codify and enumerate everything that we need from Mutability, then we can wield it safely.
Required Operations:

- Update the value
- Read the current value

These are both effectful operations.

```scala mdoc
import zio.ZIO

trait RefZ[A]:
  def get: ZIO[Any, Nothing, A]
  def update(a: A => A): ZIO[Any, Nothing, Unit]
```

Less obviously, we also need to create the Mutable reference itself.
We are changing the world, by creating a space that we can manipulate.
This operation can live in the companion object:

```scala mdoc
object RefZ:
  def make[A](a: A): ZIO[Any, Nothing, RefZ[A]] =
    ???
```

In order to confidently use this, we need certain guarantees about the behavior:

- The underlying value cannot be changed during a read
- Multiple writes cannot happen concurrently, which would result in lost updates

## Unreliable Counting

```scala mdoc
import mdoc.unsafeRunPrettyPrint

// This is lazy *purely* to silence the mdoc output.
// TODO Decide whether it's clearer to do this, or capture everything in an object
lazy val unreliableCounting =
  var counter = 0
  val increment =
    ZIO.succeed {
      counter = counter + 1
    }

  for _ <-
      ZIO
        .foreachParDiscard(Range(0, 100000))(_ =>
          increment
        )
  yield "Final count: " + counter

unsafeRunPrettyPrint(unreliableCounting)
```

Due to the unpredictable nature of shared mutable state, we do not know exactly what the final count above is.
Each time we publish a copy of this book, the code is re-executed and a different wrong result is generated.
However, conflicts are extremely likely, so some of our writes get clobbered by others, and we end up with less than the expected 100,000.
Ultimately, we lose information with this approach.

```
TODO Consider making a diagram parallel writes
```
Performing our side effects inside ZIO's does not magically make them safe.
We need to fully embrace the ZIO components, utilizing `Ref` for correct mutation.

## Reliable Counting

```scala mdoc
import zio.Ref
lazy val reliableCounting =
  def incrementCounter(counter: Ref[Int]) =
    counter.update(_ + 1)

  for
    counter <- Ref.make(0)
    _ <-
      ZIO
        .foreachParDiscard(Range(0, 100000))(_ =>
          incrementCounter(counter)
        )
    finalResult <- counter.get
  yield "Final count: " + finalResult

unsafeRunPrettyPrint(reliableCounting)
```
Now we can say with full confidence that our final count is 100000.
Additionally, these updates happen _without blocking_.
This is achieved through a strategy called "Compare & Swap", which we will not cover in detail.
*TODO Link/reference supplemental reading*

Although there are significant advantages; a basic `Ref` is not the solution for everything.
We can only pass pure functions into `update`.
The API of the plain Atomic `Ref` steers you in the right direction by not accepting `ZIO`s as parameters to any of its methods.
To demonstrate why this restriction exists, we will deliberately undermine the system by sneaking in a side effect.
First, we will create a helper function that imitates a long-running calculation.

```scala mdoc
def expensiveCalculation() = Thread.sleep(35)
```

Our side effect will be a mock alert that is sent anytime our count is updated:
```scala mdoc
def sendNotification() =
  println("Alert: We have updated our count!")
```

```scala mdoc
lazy val sideEffectingUpdates =
  for
    counter <- Ref.make(0)
    _ <-
      ZIO.foreachParDiscard(Range(0, 4))(_ =>
        counter.update { previousValue =>
          expensiveCalculation()
          sendNotification()
          previousValue + 1
        }
      )
    finalResult <- counter.get
  yield "Final count: " + finalResult

// Mdoc/this function is showing the notifications, but not the final result
unsafeRunPrettyPrint(sideEffectingUpdates)
```
What is going on?!
Previously, we were losing updates because of unsafe mutability.
Now, we have the opposite problem!
We are sending far more alerts than intended, even though we can see that our final count is 4.

*TODO This section will need significant attention and polish*

Now we must consider the limitations of the "Compare & Swap" system.
It achieves lock-free performance by letting each fiber freely make their updates, and then doing a last-second check to see if the underlying value changed during its update.
If the value has not changed, the update is made.
If it has changed, then the entire function that was passed into `update` is re-executed until it completes with a stable value.
The higher the parallelism, or the longer the operation takes, the higher the likelihood of a compare-and-swap retry.

This retry behavior is safe with pure functions, which can be executed an arbitrary number of times.
However, it is completely inappropriate for effects, which should only be executed a single time.
For these situations, we need a specialized variation of `Ref`


## Ref.Synchronized

`Ref.Synchronized` guarantees only a single execution of the `update` body and any of the effects contained inside.
The only change required is replacing `Ref.make` with `Ref.Synchronized.make`

```scala mdoc
lazy val sideEffectingUpdatesSync =
  for
    counter <- Ref.Synchronized.make(0)
    _ <-
      ZIO.foreachParDiscard(Range(0, 4))(_ =>
        counter.update { previousValue =>
          expensiveCalculation()
          sendNotification()
          previousValue + 1
        }
      )
    finalResult <- counter.get
  yield "Final count: " + finalResult

unsafeRunPrettyPrint(sideEffectingUpdatesSync)
```

Now we see exactly the number of alerts that we expected.
This correctness comes with a cost though, as the name of this type implies.
Each of your updates will run sequentially, despite initially launching them all in parallel.
This is the only known way to avoid retries.
Try to structure your code to minimize the coupling between effects and updates, and use this type only when necessary.