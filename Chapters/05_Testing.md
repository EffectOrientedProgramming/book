# Testing

Testing requires predictability, but Effects that use external systems are unpredictable.
To create predictable tests for Effects, we swap the external systems with our own, controlled, sources.
We can do this because Effects are isolated and accessible, and because they delay execution.

Anything an Effect needs (from the system or the environment) can be substituted in tests for something predictable.
For example, an Effect that fetches users from a database can use simulated data without creating a test database.

To easily replace external systems during testing, we supply a substitute system via a `ZLayer` (covered in the [Initialization](04_Initialization.md) chapter).
The `provide` method proxies different `ZLayer` resources for different scenarios: testing, debugging, running normally, etc.

## Basic ZIO Testing

A test must return an `Assertion`.
To create a test, import elements from `zio.test` and create an `object` that extends `ZIOSpecDefault` with a `spec` function:

```scala 3 mdoc:compile-only
import zio.test.*

object Basic extends ZIOSpecDefault:
  def spec =
    test("basic"):
      assertTrue(1 == 1)
```
The `String` argument to `test` is displayed in the test report.

Note that the final expression is the `Assertion` `assertTrue`.
Because of its power and flexibility, you'll probably use `assertTrue` most of the time.
However, there are numerous other `Assertion`s in the [test library](https://effectorientedprogramming.com/resources/zio/test-assertion-library).

For this book we've created an abbreviation, so our examples will instead look like this:

```scala 3 mdoc:testzio
import zio.*
import zio.test.*

def spec =
  test("basic"):
    assertTrue(1 == 1)
```

`Assertion`s that *do not* appear at the end of the test are ignored:

```scala 3 mdoc:testzio
import zio.*
import zio.test.*

def spec =
  test("Only the last assertTrue matters"):
    assertTrue(1 != 1) // Ignored
    assertTrue(1 == 1)
```

You can, however, put multiple Boolean evaluations within a single `assertTrue` by separating them with commas:

```scala 3 mdoc:testzio
import zio.*
import zio.test.*

def spec =
  test("Multiple Boolean expressions"):
    assertTrue(1 == 1, 2 == 2, 3 == 3)
```

You can also combine multiple `assertTrue` expressions using `&&`, `||` and `!`:

```scala 3 mdoc:testzio
import zio.*
import zio.test.*

def spec =
  test("Combine using operators"):
    assertTrue(1 == 1) ||
    assertTrue(2 == 2) &&
    !assertTrue(42 == 47)
```

A test can be an Effect as long as the final expression of that Effect is an `Assertion`.
The Effect is automatically run by the test framework:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.*

def spec =
  test("Effect as test"):
    defer:
      printLine("This Effect is a test").run
      assertCompletes
```

The `defer` produces an Effect that runs the `printLine` and returns `assertCompletes`.
`assertCompletes` unconditionally indicates that everything ran successfully.

We can assign the Effect to a `val` and use that as the test:

```scala 3 mdoc:silent testzio
import zio.*
import zio.direct.*
import zio.test.*

val aTest =
  defer:
    printLine("This is aTest").run
    assertCompletes
```

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.*

def spec =
  test("aTest"):
    aTest
```

Tests are typically collected into a `suite`. 
The tests in a `suite` run as a group:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.*

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

Suppose you want to build birdhouses as fast as possible, and you have a choice of materials for building them.
The `Material`s have different levels of `brittleness` so some `Tool`s might break some `Material`s.
We want to test different `Material`s with different combinations of `Tool`s.
All `Material`s and `Tool`s have `ZLayer` services, so we easily swap them around during testing.

`Wood` and `Plastic` are `Material`s with different `brittleness` factors:

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
Each type of `Tool` has an `intensity` that relates it to `Material.brittleness`.

```scala 3 mdoc:silent testzio
import zio.*
import zio.Console.*

trait Tool:
  val intensity: Int

trait Saw extends Tool
case class HandSaw() extends Saw:
  val intensity = 4
case class RoboSaw() extends Saw:
  val intensity = 8

object Saw:
  val hand    = ZLayer.succeed(HandSaw())
  val robotic = ZLayer.succeed(RoboSaw())

trait Nailer extends Tool
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
import zio.test.*

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
import zio.test.*

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
      ),
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

### The Console

When using any ZIO output facilities such as `printLine`, output is automatically captured by the `TestConsole`.
The captured output is always available from `TestConsole.output`, which produces a `Vector` of `String`.
Each element of the `Vector` contains an output line:

```scala 3 mdoc:testzio
import zio.test.*

def spec =
  test("Capture output"):
    defer:
      printLine("Morty").run
      val out1 = TestConsole.output.run
      printLine("Beth").run
      val out2 = TestConsole.output.run
      printLine(s"$out1\n****\n$out2").run
      printLine(out2(1)).run
      assertCompletes
```

As usual, `printLine` displays on the console, so we see "Morty" and "Beth" as expected.
However, the console output is also being captured into `TestConsole.output`.
Displaying `out1` produces a `Vector` containing `Morty` plus a newline (inserted by `printLine`).
Displaying `out2` gives us that same `Vector` with an additional entry: `Beth` plus a newline.
We index into the `Vector` `out2`, selecting element one, producing `Beth`.

To artificially provide your own input to the console, use `TestConsole.feedLines`:

```scala 3 mdoc:testzio
val spec =
  test("Substitute input"):
    defer:
      TestConsole.feedLines("Morty", "Beth").run
      val input = readLine.run
      printLine(input).run
      val output = TestConsole.output.run
      printLine(output).run      
      assertTrue(input == "Morty")
```

Each argument to `feedLines` becomes an input line.
We've only called `readLine` once, so only `Morty` appears, while `"Beth"` remains unread.
We can also display the input captured by `TestConsole.output`, which is always tracking the output whether we use it or not.

### Randomness

Randomness is intentionally unpredictable.
When testing, we treat randomness as an Effect and swap in our own sequence of numbers.
In the following `coinToss` function, you can guess that `Random.nextBoolean` comes from the ZIO library.
The `.run` at the end tells you that this must be an Effect, and not a call to `Scala.util.Random`:

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

The `if` looks at whether `nextBoolean` is `true` or `false` and produces `succeed("Heads")` or `fail("Tails")` accordingly.

Now we use `coinToss` to fill a `List` with five tosses.
`collectAllSuccesses` tells us how many of those were `ZIO.succeed("Heads")`.
Note that `collectAllSuccesses` is not looking at `true` or `false` values, but rather `succeed` vs. `fail` objects:

```scala 3 mdoc:silent
import zio.{Console, *}
import zio.direct.*

val flipFive =
  defer:
    val numHeads =
      ZIO
        .collectAllSuccesses:
          List.fill(5):
            coinToss
        .run
        .size
    printLine(s"Num Heads = $numHeads").run
    numHeads
```

Running this as a normal program, we see the expected random assortment of `Heads` and `Tails`:

```scala 3 mdoc:runzio
import zio.*

def run =
  flipFive
```

`Random.nextBoolean` fetches the next random Boolean value from the source of randomness.
With `Scala.util.Random` this source is hardwired into the function, but with `ZIO.Random` we can feed numbers to the function using `TestRandom.feedBooleans`:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.*

def spec =
  test("flips 5 times"):
    defer:
      TestRandom
        .feedBooleans(
          true, true, false, true, false,
        )
        .run
      val heads = flipFive.run
      assertTrue(heads == 3)
```

`feedBooleans` provides its argument values to `Random.nextBoolean`. 

Notice that our use of `TestRandom.feedBooleans` seems completely disconnected from `coinToss`, which is used via `flipFive`.
But `coinToss` is asking for random numbers, and `TestRandom.feedBooleans` provides them.
Even though the Effect is buried in two other functions, it can still be accessed here, and its behavior controlled to produce a consistent test.

In the absence of `TestRandom.feedBooleans`, `coinToss` just uses the normal `ZIO.Random` generator.
Those values are typically provided to `ZIO.Random` by calling `Scala.util.Random`.

If something in your system needs random numbers, you can use the default behavior, or you can provide your own sequence using `TestRandom`.
When your program treats randomness as an Effect, testing unusual scenarios is straightforward--you transparently provide data that produces deterministic behavior.

### Time

Using the clock is an Effect: when testing, the time can be simulated.
Suppose we want to parse the accumulated CSV files from each day, every 24 hours:

```scala 3 mdoc:silent 
import zio.*

val nightlyBatch =
  ZIO.sleep(24.hours).debug("Parsing CSV")
```

When using ZIO Test, the clock does not move forward on its own; you must explicitly change it.
Unless you move the time forward, calling a time-based Effect like `timeout` hangs indefinitely with the message:

```terminal
Warning: A test is using time, but is not
advancing the test clock, which may result
in the test hanging.  Use TestClock.adjust
to manually advance the time.
```

To advance the `TestClock`, call `adjust`:

```scala 3 mdoc:silent testzio
import zio.*

val timeTravel =
  TestClock.adjust:
    24.hours
```

It won't work to call `adjust` before or after we execute `nightlyBatch`.
We must move the clock *while `nightlyBatch` is running*--that is, *in parallel*.
This is accomplished with `zipPar`:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.*

def spec =
  test("batch runs after 24 hours"):
    defer:
      nightlyBatch.zipPar(timeTravel).run
      assertCompletes
```

Here, `zipPar` runs the `nightlyBatch` and `timeTravel` operations in parallel. 
This ensures that the `nightlyBatch` Effect completes by moving the clock forward 24 hours.
This test runs in real-world milliseconds instead of an entire day.

Using a simulated Clock means we no longer rely on real-world time.
Tests are also more predictable because time adjustments are fully controlled.

#### Targeting Failure-Prone Time Bands

Using real-world time can be error-prone because Effects can have unexpected results in certain time bands.
Consider fetching a time that happens to be 23:59:59.
After performing some operations, you get database records for the current day.
Those records may no longer be from the day associated with previously received records.
This scenario can be very hard to test when using real-world time.
With a simulated clock, your tests can adjust the clock to reliably reproduce the test conditions.
