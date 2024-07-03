# Initialization

Initializing an application from values provided at startup is a perennial challenge in software.
Solutions are diverse, impressive, and often completely bewildering.

One reason to modularize an application into parts is that the relationship between those parts can be expressed and changed based on a particular path of execution through the program.  
Breaking things into parts and expressing what they need is commonly called *Dependency Inversion*. 

Dependency Inversion enables *Dependency Injection* which produces more flexible code.
Instead of manually constructing and passing all dependencies through the application,  an "Injector" automatically provides instances of those dependencies when they are needed.

Understanding these terms is not crucial for writing Effect Oriented code, but helps when building the `Layer`s for your application.
{{TODO: Do we need to describe “layers”? }}

Common approaches to implement Dependency Injection rely on runtime magic (e.g. reflection) and require everything to be created through a Dependency Injection manager (the “Injector”). This complicates construction and can make it difficult or impossible to express dependencies at compile time.

If we instead express dependencies through the type system, the compiler can verify that the needed parts are available given a particular path of execution (main app, test suite one, test suite two, etc.).

## Effects and Dependencies

The powers that Effects give you can also be used during initialization.


With ZIO's approach to dependencies, you get many desirable characteristics at compile-time, using plain function calls.
Your services are defined as classes with constructor arguments, just as in any vanilla Scala application.

For any given service in your application, you define what it needs in order to execute.
Finally, when it is time to build your application, all of these pieces can be provided in one, flat space.
Each component automatically finds its dependencies, and makes itself available to other components that need it.

Dependency cycles are not allowed by ZIO—you cannot build a program where `A` depends on `B`, and `B` depends on `A`.
You are alerted at compile time about illegal cycles.

ZIO’s dependency management provides capabilities that are not possible in other approaches, such as sharing a single instance of a dependency across multiple test classes, or even multiple applications.

## Let's Make Bread

To illustrate how ZIO can assemble our programs, we make and eat `Bread`. [^footnote]
[^footnote]: Although we are utilizing very different tools with different goals, we were inspired by Li Haoyi's excellent article ["What's Functional Programming All About?"](https://www.lihaoyi.com/post/WhatsFunctionalProgrammingAllAbout.html)

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

trait Bread:
  def eat =
    Console.printLine:
      "Bread: Eating"
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class BreadStoreBought() extends Bread

object BreadStoreBought:
  val layer =
    ZLayer.succeed:
      BreadStoreBought()
```

{{ TODO: Needs some refactoring }}

In this book, we follow the Scala practice of preferring `case` classes over ordinary classes.
`case` classes are immutable by default and automatically provide commonly-needed functionality.

The companion object `Dough` creates a `ZLayer`, which you can think of as a more-powerful constructor. 
Calling `fresh` produces a new instance of `Dough` within a `ZLayer`.
This `ZLayer` provides the `Dough` instance as a dependency to any other Effect that needs it.

Looking at the code from the inside out, the `defer` block executes the `printLine` Effect by calling `.run`, but does *not* call `.run` for `Dough`. 
The result of the `defer` is an Effect, because `defer` always produces an Effect.
In ZIO all managed Effects are contained in ZIO objects.
Thus, all Effectful functions return a ZIO object.

This ZIO is passed to `ZLayer.fromZIO` which produces a `ZLayer` object (which is also a ZIO) containing a `Dough` object.

## Provide Dependencies

{{ TODO: Move up to initial example }}

`Dough.fresh` produces a `ZLayer` containing a `Dough` for use as a dependency by other Effects.
How is that dependency provided?

If an Effect requires a dependency
We must provide all required dependencies to an Effect before you can run it.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[Bread]:
      bread => bread.eat
    .provide:
      BreadStoreBought.layer
```

## Missing Dependencies

If the dependency for an Effect isn't provided, we get a compile error:

```scala 3 mdoc:fail
ZIO
  .serviceWithZIO[Dough]:
    dough => dough.letRise
  .provide()
```

## Dependencies From Effects

{{ TODO: now let's make bread from components }}

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

// todo: explain the letRise being an Effect, i.e. dependencies can themselves be Effects
class Dough():
  val letRise =
    Console.printLine:
      "Dough is rising"
```


```scala 3 mdoc:silent
import zio.*
import zio.direct.*

object Dough:
  val fresh =
    ZLayer.fromZIO:
      defer:
        Console.printLine("Dough: Mixed").run
        Dough()
```

## Automatically Assemble Dependencies

The requirements for each ZIO operation are tracked and combined automatically.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class Heat()

val oven =
  ZLayer.fromZIO:
    defer:
      Console.printLine("Oven: Heated").run
      Heat()
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class BreadHomeMade(
    heat: Heat,
    dough: Dough
) extends Bread

object Bread:
  val homemade =
    ZLayer.fromZIO:
      defer:
        Console
          .printLine("BreadHomeMade: Baked")
          .run
        BreadHomeMade(
          ZIO.service[Heat].run,
          ZIO.service[Dough].run
        )
```

Something around how like typical DI, the "graph" of dependencies gets resolved "for you"
This typically happens in some completely new/custom phase, that does follow standard code paths.
Dependencies on Effects propagate to Effects which use Effects.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[Bread]:
      bread => bread.eat
    .provide(
      Bread.homemade,
      Dough.fresh,
      oven
    )
```

## Sharing Dependencies

Eventually, we grow tired of eating plain `Bread` and decide to start making `Toast`.
Both of these processes require `Heat`.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class Toast(heat: Heat, bread: Bread):
  val eat =
    Console.printLine:
      "Toast: Eating"

object Toast:
  val make =
    ZLayer.fromZIO:
      defer:
        Console.printLine("Toast: Made").run
        Toast(
          ZIO.service[Heat].run,
          ZIO.service[Bread].run
        )
```

It is possible to also use the oven to provide `Heat` to make the `Toast`.

The dependencies are tracked by their type.
In this case both `Toast.make` and `Bread.homemade` require `Heat`.

Notice - Even though we provide the same dependencies in this example, oven is _also_ required by `Toast.make`

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .service[Toast]
    .provide(
      Toast.make,
      Bread.homemade,
      Dough.fresh,
      oven
    )
```

However, the oven uses a lot of energy to make `Toast`.
It would be great if we can instead use our dedicated toaster!

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val toaster =
  ZLayer.fromZIO:
    defer:
      Console
        .printLine("Toaster: Heated")
        .run
      Heat()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .service[Heat]
    .provide:
      toaster
```

## Unique Dependencies

```scala 3 mdoc:fail
ZIO
  .service[Toast]
  .provide(
    Toast.make,
    Dough.fresh,
    Bread.homemade,
    oven,
    toaster
  )
```

Unfortunately our program is now ambiguous.
It cannot decide if we should be making `Toast` in the oven, `Bread` in the toaster, or any other combination.

## Disambiguating Dependencies

If you find discover that your existing types produce ambiguous dependencies, introduce more specific types.
In our case, we choose to distinguish our `Heat` sources, so that they are only used where they are intended.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class Toaster()
object Toaster:
  val layer =
    ZLayer.fromZIO:
      defer:
        Console
          .printLine("Toaster: Heating")
          .run
        Toaster()
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class ToastZ(
    heat: Toaster,
    bread: Bread
):
  val eat =
    Console.printLine:
      "Toast: Eating"

object ToastZ:
  val make =
    ZLayer.fromZIO:
      defer:
        Console.printLine("ToastZ: Made").run
        ToastZ(
          ZIO.service[Toaster].run,
          ZIO.service[Bread].run
        )
```

We can explicitly provide dependencies when needed, to prevent ambiguity.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[ToastZ]:
      toast => toast.eat
    .provide(
      ToastZ.make,
      Toaster.layer,
      Bread.homemade,
      Dough.fresh,
      oven
    )
```

Author Note: Hardcoded, because mdoc doesn't properly support the `ZLayer.Debug.tree` output.

Output: 

```terminal
[info]   ZLayer Wiring Graph  
[info] ◉ ToastZ.make
[info] ├─◑ Toaster.layer
[info] ╰─◑ Bread.homemade
[info]   ├─◑ oven
[info]   ╰─◑ Dough.fresh
```

## Dependency Cleanup

So far, we have focused on providing `Layer`s to Effects, but this can also go the other way!
If an Effect already has no outstanding dependencies, it can be used to construct a `Layer`.

We can use this to correct a dangerous oversight in our scenarios.
We heat up our oven, but then never turn it off!
We can build an oven that turns itself off when it is no longer needed.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val ovenSafe =
  ZLayer.fromZIO:
    ZIO
      .succeed(Heat())
      .tap:
        _ =>
          Console.printLine:
            "Oven: Heated"
      .withFinalizer:
        _ =>
          Console
            .printLine:
              "Oven: Turning off!"
            .orDie
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[Bread]:
      bread => bread.eat
    .provide(
      Bread.homemade,
      Dough.fresh,
      ovenSafe,
      Scope.default
    )
```

## Construction Failure

Since dependencies can be built with Effects, this means that they can fail.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

case class BreadFromFriend() extends Bread()
object Friend:
  def forcedFailure(invocations: Int) =
    defer:
      Console
        .printLine(
          s"Attempt $invocations: Failure(Friend Unreachable)"
        )
        .run
      ZIO
        .when(true)(
          ZIO.fail(
            "Failure(Friend Unreachable)"
          ) // TODO: Replace error with failure pervasively
        )
        .as(???)
        .run
      ZIO.succeed(BreadFromFriend()).run

  def bread(worksOnAttempt: Int) =
    var invocations =
      0
    ZLayer.fromZIO:
      invocations += 1
      if invocations < worksOnAttempt then
        forcedFailure(invocations)
      else if invocations == 1 then
        ZIO.succeed(BreadFromFriend())
      else
        Console
          .printLine(
            s"Attempt $invocations: Succeeded"
          )
          .orDie
          .as:
            BreadFromFriend()
end Friend
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .service[Bread]
    .provide:
      Friend.bread(worksOnAttempt =
        3
      )
```

## Fallback Dependencies

We can rely on this method of acquiring `Bread`, but we are going to be paying for that convenience.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .service[Bread]
    .provide:
      Friend
        .bread(worksOnAttempt =
          3
        )
        .orElse:
          BreadStoreBought.layer
```

## Retries

```scala 3 mdoc
import zio.*
import zio.direct.*

def logicWithRetries(retries: Int) =
  ZIO
    .serviceWithZIO[Bread]:
      bread => bread.eat
    .provide:
      Friend
        .bread(worksOnAttempt =
          3
        )
        .retry:
          Schedule.recurs:
            retries
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  logicWithRetries(retries =
    2
  )
```

## External Configuration

Our programs often need to change their behavior based on the environment during startup.
Three typical ways are:

- Command Line Parameters
- Configuration Files
- OS Environment Variables

We can use the [ZIO Config library](https://github.com/zio/zio-config) to read these.
This is one of the few additional libraries used in this book on top of core ZIO.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import zio.config.*
```

This import brings in most of the core Config datatypes and functions that we need.
Next we make a case class that will hold our values.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class RetryConfig(times: Int)
```

To automatically map values in config files to our case class, we import a macro from the zio-config "magnolia" module.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import zio.config.magnolia.deriveConfig

val configDescriptor: Config[RetryConfig] =
  deriveConfig[RetryConfig]
```

It is heavily modularized so that you only pull in the integrations for the technologies used in your project.
We want to use the Typesafe config format, so we import everything from that module.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import zio.config.typesafe.*
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val configProvider =
  ConfigProvider.fromHoconString:
    "{ times: 2 }"

val config =
  ZLayer.fromZIO:
    read:
      configDescriptor.from:
        configProvider
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[RetryConfig]:
      retryConfig =>
        logicWithRetries(retries =
          retryConfig.times
        )
    .provide:
      config
```

Now we have bridged the gap between our logic and configuration files.
This was a longer detour than our other steps, but a common requirement in real-world applications.

## Test Dependencies

Effects need access to external systems thus are unpredictable.  
It is great to have these abilities for responding to the messiness of the real world, but we want to be able to test our programs in a predictable way.
So how do we write tests for Effects that are predictable?
With ZIO we can replace the external systems with predictable ones when running our tests.
Rather than actually trying to get bread from our living, fallible friend, we can create an ideal friend that will always give us `Bread`.

```scala 3 mdoc
object IdealFriend:
  val bread =
    ZLayer.succeed:
      BreadFromFriend()
```

We take another brief detour into `zio-test`, to provide just enough context to understand the tests.

In `zio-test`, we build tests that are Effects that return an `Assertion`.
We will do this incrementally, starting with some logic.

```scala 3 mdoc:silent testzio
import zio.*
import zio.direct.*

import zio.test.assertTrue

val logic =
  defer:
    assertTrue(1 == 1)
```

Next, we turn it into a test case by giving it a name via the `test` function.

```scala 3 mdoc:silent testzio
import zio.*
import zio.direct.*

import zio.test.test

val testCase =
  test("eat Bread"):
    logic
```

Finally, we assign it to the `spec` field of a test class. [^footnote]
[^footnote]: In real test code, you would be using `ZIOSpecDefault`, or one of the other `ZIOSpec*` variants. We use our custom test runner here for brevity.

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*

def spec =
  testCase
```

Historically, when call we call `println`, that output disappears into the void.
`zio-test` provides us a `TestConsole`, which captures all the output produced during a test.
This allows us to make assertions on something that is typically a black hole of our code.

Armed with these tools, we can now return to the kitchen to test our `Bread` eating with our ideal friend.

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*

import zio.test.*

def spec =
  test("eat Bread"):
    defer:
      ZIO
        .serviceWithZIO[Bread]:
          bread => bread.eat
        .run
      val output =
        TestConsole.output.run
      assertTrue(
        output.contains("Bread: Eating\n")
      )
  .provide:
    IdealFriend.bread
```

## Testing Effects

For user-defined types, calling `.provide` with your test implementation is the way to go.
However, for built-in types like `Console`, `Random`, and `Clock`, ZIO Test provides special APIs.
We will demonstrate a few, but not exhaustively cover them.

### Random

An example of this is Random numbers.  Randomness is inherently unpredictable.  But in ZIO Test, without changing our Effects we can change the underlying systems with something predictable:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val coinToss =
  defer:
    if Random.nextBoolean.run then
      ZIO.debug("Heads").run
      ZIO
        .succeed:
          "Heads"
        .run
    else
      ZIO.debug("Tails").run
      ZIO
        .fail:
          "Tails"
        .run
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
import zio.direct.*

def run =
  flipTen
```

```scala 3 mdoc:testzio
import zio.test.*

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

The `Random` Effect uses an injected something which when running the ZIO uses the system's unpredictable random number generator.  In ZIO Test the `Random` Effect uses a different something which can predictably generate "random" numbers.  `TestRandom` provides a way to define what those numbers are.  This example feeds in the `Int`s `1` and `2` so the first time we ask for a random number we get `1` and the second time we get `2`.

Anything an Effect needs (from the system or the environment) can be substituted in tests for something predictable.  For example, an Effect that fetches users from a database can be simulated with a predictable set of users instead of having to setup a test database with predictable users.

When your program treats randomness as an Effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

### Time

Even time can be simulated as using the clock is an Effect.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val nightlyBatch =
  ZIO
    .sleep:
      24.hours
    .debug:
      "Parsing CSV"
```

```scala 3 mdoc:testzio
import zio.test.*

def spec =
  test("batch runs after 24 hours"):
    val timeTravel =
      TestClock.adjust:
        24.hours

    defer:
      nightlyBatch.zipPar(timeTravel).run
//      val fork =
//        nightlyBatch.fork.run
//      timeTravel.run
//      fork.join.run

      assertCompletes

// TODO: update prose on zip
```

The `race` is between `nightlyBatch` and `timeTravel`.
It completes when the first Effect succeeds and cancels the losing Effect, using the Effect System's cancellation mechanism.

By default, in ZIO Test, the clock does not change unless instructed to.
Calling a time based Effect like `timeout` would hang indefinitely with a warning like:

```terminal
Warning: A test is using time, but is not 
advancing the test clock, which may result 
in the test hanging.  Use TestClock.adjust 
to manually advance the time.
```

To test time based Effects we need to `fork` those Effects so that then we can adjust the clock.
After adjusting the clock, we can then `join` the Effect where in this case the timeout has then been reached causing the Effect to return a `None`.

Using a simulated Clock means that we no longer rely on real-world time for time.
So this example runs in milliseconds of real-world time instead of taking an actual 1 second to hit the timeout.
This way our time-based tests run much more quickly since they are not based on actual system time.
They are also more predictable as the time adjustments are fully controlled by the tests.

#### Targeting Failure-Prone Time Bands

Using real-world time also can be error prone because Effects may have unexpected results in certain time bands.
For instance, if you have code that gets the time and it happens to be 23:59:59, then after some operations that take a few seconds, you get some database records for the current day, those records may no longer be the day associated with previously received records.  This scenario can be very hard to test for when using real-world time.  When using a simulated clock in tests, you can write tests that adjust the clock to reliably reproduce the condition.
