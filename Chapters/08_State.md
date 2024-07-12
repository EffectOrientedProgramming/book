# State

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

Both of these operations are Effects.
In order to confidently use them, we need certain guarantees about the behavior:

- The underlying value cannot be changed during a read
- Multiple writes cannot happen concurrently, which would result in lost updates

Less obviously, we also need to create the Mutable reference itself.
We are changing the world, by creating a space that we can manipulate.

## Unreliable State

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val unreliableCounting =
  var counter = 0
  val increment =
    ZIO.succeed:
      counter = counter + 1

  defer:
    ZIO
      .foreachParDiscard(Range(0, 100000)):
        _ => increment
      .run
    // It's not obvious to the reader why
    // we need to wrap counter in .succeed
    "Final count: " +
      ZIO.succeed(counter).run
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  unreliableCounting
```

Due to the unpredictable nature of shared mutable state, we do not know exactly what the final count above is.
Each time we publish a copy of this book, the code is re-executed and a different wrong result is generated.
However, conflicts are extremely likely, so some of our writes get clobbered by others, and we end up with less than the expected 100,000.
Ultimately, we lose information with this approach.

Performing our Side Effects inside ZIO's does not magically make them safe.
We need to fully embrace the ZIO components, utilizing `Ref` for correct mutation.

## Reliable State

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

lazy val reliableCounting =
  def incrementCounter(counter: Ref[Int]) =
    counter.update:
      _ + 1

  defer:
    val counter = Ref.make(0).run
    ZIO
      .foreachParDiscard(Range(0, 100000)):
        _ =>
          incrementCounter:
            counter
      .run
    "Final count: " + counter.get.run

def run =
  reliableCounting
```

Now we can say with full confidence that our final count is 100000.
Additionally, these updates happen _without blocking_.
This is achieved through a strategy called "Compare & Swap", which we will not cover in detail.

## Unreliable Effects

Although there are significant advantages; a basic `Ref` is not the solution for everything.
We can only pass pure functions into `update`.
The API of the plain Atomic `Ref` steers you in the right direction by not accepting `ZIO`s as parameters to any of its methods.
To demonstrate why this restriction exists, we will deliberately undermine the system by sneaking in a Side Effect.
First, we will create a helper function that imitates a long-running calculation.

```scala 3 mdoc
import zio.*
import zio.direct.*

def expensiveCalculation() =
  Thread.sleep:
    35
```

Our Side Effect will be a mock alert that is sent anytime our count is updated:

```scala 3 mdoc
import zio.*
import zio.direct.*

def sendNotification() =
  println:
    "Alert: updating count!"
```

```scala 3 mdoc
import zio.*
import zio.direct.*

def update(counter: Ref[Int]) =
  counter.update:
    previousValue =>
      expensiveCalculation()
      sendNotification()
      previousValue + 1
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    val counter = Ref.make(0).run
    ZIO
      .foreachParDiscard(Range(0, 4)):
        _ => update(counter)
      .run
    val finalCount = counter.get.run
    s"Final count: $finalCount"
```

What is going on?!
Previously, we were losing updates because of unsafe mutability.
Now, we have the opposite problem!
We are sending far more alerts than intended, even though we can see that our final count is 4.

Now we must consider the limitations of the "Compare & Swap" system.
It achieves lock-free performance by letting each Effect freely make their updates, and then doing a last-second check to see if the underlying value changed during its update.
If the value has not changed, the update is made.
If it has changed, then the entire function that was passed into `update` is re-executed until it completes with a stable value.
The higher the parallelism, or the longer the operation takes, the higher the likelihood of a compare-and-swap retry.

This retry behavior is safe with pure functions, which can be executed an arbitrary number of times.
However, it is completely inappropriate for Effects, which should only be executed a single time.
For these situations, we need a specialized variation of `Ref`

## Reliable Effects

`Ref.Synchronized` guarantees only a single execution of the `update` body and any of the Effects contained inside.
The only change required is replacing `Ref.make` with `Ref.Synchronized.make`

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val sideEffectingUpdatesSync =
  defer:
    val counter =
      Ref.Synchronized.make(0).run
    ZIO
      .foreachParDiscard(Range(0, 4)):
        _ => update(counter)
      .run
    val finalCount = counter.get.run
    s"Final count: $finalCount"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  sideEffectingUpdatesSync
```

Now we see exactly the number of alerts that we expected.
This correctness comes with a cost though, as the name of this type implies.
Each of your updates will run sequentially, despite initially launching them all in parallel.
This is the only known way to avoid retries.
Try to structure your code to minimize the coupling between Effects and updates, and use this type only when necessary.
