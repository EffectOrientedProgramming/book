# Testing

Testing requires predictability, but Effects that use external systems are unpredictable.
To create predictable tests for Effects, we swap the external systems with our own, controlled, sources.
We can do this because Effects are isolated and accessible, and because they delay execution.

To easily replace external systems during testing, we supply that external system via a `ZLayer` (covered in the [Initialization](04_Initialization.md) chapter).
The `provide` method proxies different `ZLayer` resources for different scenarios: testing, debugging, running normally, etc.

## Basic ZIO Testing

A test must return an `Assertion`.
To create a test, import elements from `zio.test` and create an `object` that extends `ZIOSpecDefault` with a `spec` function:

```scala 3 mdoc:compile-only
import zio.test.{test, *}

object Basic extends ZIOSpecDefault:
  def spec =
    test("basic"):
      assertTrue(1 == 1)
```
The `String` argument to `test` is displayed in the test report.

Note that the final expression is the assertion `assertTrue`.
Because of its power and flexibility, you'll probably use `assertTrue` most of the time.
However, there are numerous other assertions in the [test library](https://effectorientedprogramming.com/resources/zio/test-assertion-library).

For this book we've created an abbreviation, so our examples will instead look like this:

```scala 3 mdoc:testzio
import zio.*
import zio.test.{test, *}

def spec =
  test("basic"):
    assertTrue(1 == 1)
```

Assertions that *do not* appear at the end of the test are ignored:

```scala 3 mdoc:testzio
import zio.*
import zio.test.{test, *}

def spec =
  test("Only the last assertTrue matters"):
    assertTrue(1 != 1) // Ignored
    assertTrue(1 == 1)
```

You can, however, put multiple Boolean evaluations within a single `assertTrue` by separating them with commas:

```scala 3 mdoc:testzio
import zio.*
import zio.test.{test, *}

def spec =
  test("Multiple Boolean expressions"):
    assertTrue(1 == 1, 2 == 2, 3 == 3)
```

You can also combine multiple `assertTrue` expressions using `&&`, `||` and `!`:

```scala 3 mdoc:testzio
import zio.*
import zio.test.{test, *}

def spec =
  test("Combine using operators"):
    assertTrue(1 == 1) ||
    assertTrue(2 == 2) &&
    !assertTrue(42 == 47)
```

A test can be an Effect as long as the final expression of that Effect is an assertion.
The Effect is automatically run by the test framework:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, *}

def spec =
  test("Effect as test"):
    defer:
      printLine("This test is an Effect").run
      assertCompletes
```

The `defer` produces an Effect that runs the `printLine` and returns `assertCompletes`.
`assertCompletes` unconditionally indicates that everything ran successfully.

We can assign the Effect to a `val` and use that as the test:

```scala 3 mdoc:silent testzio
import zio.*
import zio.direct.*
import zio.test.assertCompletes

val aTest =
  defer:
    printLine("This is aTest").run
    assertCompletes
```

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.test

def spec =
  test("aTest"):
    aTest
```

Tests are typically collected into a `suite`. 
The tests in a `suite` run as a group:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, *}

val bTest =
  defer:
    printLine("This is bTest").run
    assertTrue(1 == 1)

def spec =
  suite("A Suite of Tests")(
    test("aTest in Suite"):
      aTest
    ,
    test("bTest in Suite"):
      bTest,
  )
```

Tests run in parallel so the output does not necessarily appear in the order the tests are listed.

## Birdhouse Factory

Suppose you want to build birdhouses as fast as possible, and you have the choice of materials for building them.
These `Material`s have different levels of `brittleness` so some `Tool`s might break some `Material`s.
We want to test different `Material`s with different combinations of `Tool`s.
All `Material`s and `Tool`s have `ZLayer` services, so we easily swap them around during testing.

We define `Wood` and `Plastic` as `Material`s and give them different `brittleness` factors:

```scala 3 mdoc:silent testzio
import zio.*

trait Material:
  val brittleness: Int

case class Wood() extends Material:
  val brittleness = 5

case class Plastic() extends Material:
  val brittleness = 10

object Material:
  val wood    = ZLayer.succeed(Wood())
  val plastic = ZLayer.succeed(Plastic())
```

We define `Tool`s in a similar way. 
Each type of `Tool` has an `intensity` which relates it to `Material.brittleness`.

```scala 3 mdoc:silent testzio
import zio.*
import zio.Console.*

trait Tool:
  val action: String
  val intensity: Int
  val use =
    printLine(
      s"$this $action, intensity $intensity"
    )

trait Saw extends Tool:
  val action = "sawing"
case class HandSaw() extends Saw:
  val intensity = 4
case class RoboSaw() extends Saw:
  val intensity = 8

object Saw:
  val hand    = ZLayer.succeed(HandSaw())
  val robotic = ZLayer.succeed(RoboSaw())

trait Nailer extends Tool:
  val action = "nailing"
case class Hammer() extends Nailer:
  val intensity = 4
case class RoboNailer() extends Nailer:
  val intensity = 11

object Nailer:
  val hand    = ZLayer.succeed(Hammer())
  val robotic = ZLayer.succeed(RoboNailer())
````

The test takes a `Material` and checks it against a `Saw` and a `Nailer`:

```scala 3 mdoc:silent testzio
import zio.*
import zio.direct.*
import zio.test.assertTrue

val testToolWithMaterial =
  defer:
    val material = ZIO.service[Material].run
    val saw      = ZIO.service[Saw].run
    val nailer   = ZIO.service[Nailer].run
    assertTrue(
      saw.intensity < material.brittleness,
      nailer.intensity < material.brittleness,
    )
```

Notice that in `testToolWithMaterial`, all the services are unfulfilled.
This way, each `test` can `provide` different combinations of services:

```scala 3 mdoc:testzio
import zio.test.{test, *}

  def spec =
    suite("Materials with different Tools")(
      test("Wood with hand tools"):
        testToolWithMaterial.provide(
          Material.wood,
          Saw.hand,
          Nailer.hand,
        )
      ,
      test("Plastic with hand tools"):
        testToolWithMaterial.provide(
          Material.plastic,
          Saw.hand,
          Nailer.hand,
        )
      ,
      test("Plastic with robo tools"):
        testToolWithMaterial.provide(
          Material.plastic,
          Saw.robotic,
          Nailer.robotic,
        )
      ,
      test("Plastic with robo saw & hammer"):
        testToolWithMaterial.provide(
          Material.plastic,
          Saw.robotic,
          Nailer.hand,
        )
      ,
    )
```

Notice the clarity of the failure report.
It gives all the information you need to see exactly what's happening.

We've only shown a few combinations here.
As an exercise, try adding others.
Then add a new `Material` called `Metal`, and a new `Tool` category called `Power`, with an additional type `Drill`.

## Testing Effects

As you've just seen, whenever you create a user-defined type, you can include a method to produce a `ZLayer` containing an instance of that type.
To vary the types across multiple tests, supply different types to each test using `provide`.

This only works for user-defined types. 
What about built-in types like `Console`, `Random`, and `Clock`? 
For these, ZIO Test provides special APIs.
We'll look at the two most-commonly encountered ones, after `Console`: randomness and time.

### Randomness

Randomness is inherently unpredictable.
To perform testing when randomness is involved, we must treat it as an Effect and then swap in a controlled sequence of fake random numbers.
Anything an Effect needs (from the system or the environment) can be substituted in tests for something predictable.
For example, an Effect that fetches users from a database can be simulated with a predictable set of users instead of having to set up a test database with predictable users.

In the following `coinToss` function, you can guess that `Random.nextBoolean` is an Effect from the ZIO library (rather than `Scala.util.Random`) by the `.run` at the end:

```scala 3 mdoc:silent
import zio.{Console, *}
import zio.direct.*

val coinToss =
  defer:
    if Random.nextBoolean.run then
      printLine("Heads").run
      ZIO.succeed("Heads").run
    else
      printLine("Tails").run
      ZIO.fail("Tails").run
```


```scala 3 mdoc:silent
import zio.{Console, *}
import zio.direct.*

val flipTen =
  defer:
    val numHeads =
      ZIO
        .collectAllSuccesses:
          List.fill(10):
            coinToss
        .run
        .size
    printLine(s"Num Heads = $numHeads").run
    numHeads
```

```scala 3 mdoc:runzio
import zio.*

def run =
  flipTen
```

`Random.nextBoolean` fetches the next random Boolean value from the source of randomness.
With `Scala.util.Random` this source is hardwired into the function, but with `ZIO.Random` we can feed numbers to the function using `TestRandom.feedBooleans`:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, *}

def spec =
  test("flips 10 times"):
    defer:
      TestRandom
        .feedBooleans(true)
        .repeatN(9)
        .run
      assertTrue(flipTen.run == 10)
```

Notice that our use of `TestRandom.feedBooleans` seems completely disconnected from `coinToss`, which is used via `flipTen`.
But `coinToss` is asking for random numbers, and `TestRandom.feedBooleans` provides them.
In the absence of `TestRandom.feedBooleans`, `coinToss` just uses the normal random generator provided by `Scala.util.Random`.
Whenever something in your system needs random numbers, you can do nothing and get the default behavior, or you can provide your own sequence using `TestRandom`.
In this case, `feedBooleans` is just given the constant value of `true` for each iteration, but you can also provide a function.
This function could, for example, read the values from a file.

When your program treats randomness as an Effect, testing unusual scenarios becomes straightforward.
With ZIO's builtin methods, you can transparently provide random data that results in deterministic behavior.

### Time

Even time can be simulated, as using the clock is an Effect.

```scala 3 mdoc:silent
import zio.*

val nightlyBatch =
  ZIO
    .sleep(24.hours)
    .debug("Parsing CSV")
```

By default, in ZIO Test, the clock does not change unless instructed to.
Calling a time-based Effect like `timeout` would hang indefinitely with a warning like:

```terminal
Warning: A test is using time, but is not
advancing the test clock, which may result
in the test hanging.  Use TestClock.adjust
to manually advance the time.
```

We need to explicitly advance the time to make the test complete.

```terminal
import zio.*

val timeTravel =
  TestClock.adjust:
    24.hours
```

However, be aware that it is not correct to call `TestClock.adjust` before or after we execute `nightlyBatch`.
We need to adjust the clock *while `nightlyBatch` is running*.

```terminal
import zio.*
import zio.direct.*
import zio.test.{test, *}

def spec =
  test("batch runs after 24 hours"):
    defer:
      nightlyBatch.zipPar(timeTravel).run
      assertCompletes
```

By running `nightlyBatch` and `timeTravel` in parallel, we ensure that the `nightlyBatch` Effect completes after "24 hours".

Using a simulated Clock means we no longer rely on real-world time.
The example now runs in real-world milliseconds instead an entire day.
They are also more predictable as the time adjustments are fully controlled by the tests.

#### Targeting Failure-Prone Time Bands

Using real-world time also can be error-prone because Effects may have unexpected results in certain time bands.
Suppose you have code that gets the time, and it happens to be 23:59:59.
After some operations that take a few seconds, you get database records for the current day.
Those records may no longer be the day associated with previously received records.
This scenario can be very hard to test when using real-world time.
With a simulated clock, your tests can adjust the clock to reliably reproduce the test conditions.
