# Testing

Testing requires predictability.
Effects that use external systems are unpredictable.
How do we write predictable tests for Effects?
When testing, we replace external systems with predictable ones.
We can do this because Effects are isolated and accessible, and because they delay execution.

To easily replace external systems during testing, we provide that external system via a `ZLayer` (covered in the [Initialization](04_Initialization.md) chapter).
The `provide` method contains different `ZLayer` resources depending on whether we're testing, debugging, running normally, etc.

## Basic ZIO Testing

A test is something that returns an `Assertion`.
To create a test, import some elements of `zio.test` and create an `object` that extends `ZIOSpecDefault` with a `spec` function:

```scala 3 mdoc:compile-only
import zio.test.{test, assertTrue, ZIOSpecDefault}

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
import zio.test.{test, assertTrue}

def spec =
  test("basic"):
    assertTrue(1 == 1)
```

Assertions that *do not* appear at the end of the test are ignored:

```scala 3 mdoc:testzio
import zio.*
import zio.test.{test, assertTrue}

def spec =
  test("basic2"):
    assertTrue(1 != 1) // Ignored
    assertTrue(1 == 1)
```

You can, however, put multiple Boolean evaluations within a single `assertTrue` by separating them with commas:

```scala 3 mdoc:testzio
import zio.*
import zio.test.{test, assertTrue}

def spec =
  test("basic3"):
    // Multiple boolean expressions:
    assertTrue(1 == 1, 2 == 2, 3 == 3)
```

A test can be an Effect as long as the final expression is an assertion.
The Effect is automatically run by the test framework:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, assertCompletes}

def spec =
  test("basic4"):
    defer:
      printLine("testing basic4").run
      assertCompletes
```

The `defer` produces an Effect that runs the `printLine` and returns `assertCompletes`.

`assertCompletes` is another useful assertion that indicates that everything ran successfully, but without any explicit Boolean test.

We can assign the Effect to a `val` and use that as the test:

```scala 3 mdoc:silent testzio
import zio.*
import zio.direct.*
import zio.test.assertCompletes

val basic5 =
  defer:
    printLine("testing basic5").run
    assertCompletes
```

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.test

def spec =
  test("basic5"):
    basic5
```

We create *suites* of tests that all run together:

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, suite, assertTrue}

val basic6 =
  defer:
    printLine("testing basic6").run
    assertTrue(1 == 1)

def spec =
  suite("Creating a Suite of Tests")(
    test("basic5 in suite"):
      basic5
    ,
    test("basic6 in suite"):
      basic6,
  )
```

Note that these tests run in parallel so the output does not necessarily appear in the order the tests are listed.

## Testing Bread

```scala 3 mdoc:invisible
import zio.*
import zio.Console.*

trait Bread:
  def eat =
    printLine("Bread: Eating")

case class BreadFromFriend() extends Bread
```
Rather than trying to get `Bread` from a fallible human, we can create an `IdealFriend` that will always give us `Bread`.

```scala 3 mdoc:silent
import zio.*

object IdealFriend:
  val bread =
    ZLayer.succeed:
      BreadFromFriend()
```

Armed with these tools, we can now return to the kitchen to test our `Bread` eating with our ideal friend.

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, assertTrue, TestConsole}

def spec =
  test("eat Bread"):
    defer:
      ZIO
        .serviceWithZIO[Bread]:
          bread => bread.eat
        .run
      val output = TestConsole.output.run
      assertTrue:
        output.contains("Bread: Eating\n")
  .provide:
    IdealFriend.bread
```

## Testing Effects

For user-defined types, call `.provide` with your test implementation.
But for built-in types like `Console`, `Random`, and `Clock`, ZIO Test provides special APIs.
We demonstrate some of the most common.

### Random

Randomness is inherently unpredictable.
With ZIO Test, we can produce predictable random numbers for testing, but without changing any Effects:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val coinToss =
  defer:
    if Random.nextBoolean.run then
      ZIO.debug("Heads").run
      ZIO.succeed("Heads").run
    else
      ZIO.debug("Tails").run
      ZIO.fail("Tails").run
```

```scala 3 mdoc:silent
import zio.*
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
    ZIO.debug(s"Num Heads = $numHeads").run
    numHeads
```

```scala 3 mdoc:runzio
import zio.*

def run =
  flipTen
```

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*
import zio.test.{test, assertTrue, TestRandom}

def spec =
  test("flips 10 times"):
    defer:
      TestRandom
        .feedBooleans(true)
        .repeatN(9)
        .run
      assertTrue:
        flipTen.run == 10
```

The `Random` Effect uses an injected something which when running the ZIO uses the system's unpredictable random number generator.
In ZIO Test the `Random` Effect uses a different something which can predictably generate "random" numbers.
`TestRandom` provides a way to define what those numbers are.
This example feeds in the `Int`s `1` and `2` so the first time we ask for a random number we get `1` and the second time we get `2`.

Anything an Effect needs (from the system or the environment) can be substituted in tests for something predictable.
For example, an Effect that fetches users from a database can be simulated with a predictable set of users instead of having to set up a test database with predictable users.

When your program treats randomness as an Effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

### Time

Even time can be simulated, as using the clock is an Effect.

```scala 3 mdoc:silent
import zio.*

val nightlyBatch =
  ZIO
    .sleep:
      24.hours
    .debug:
      "Parsing CSV"
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
import zio.test.{test, assertCompletes, TestRandom}

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
