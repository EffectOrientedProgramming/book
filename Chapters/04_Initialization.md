# Initialization

```
TODO: Rewrite 'Initialization' intro material once I understand it by working through the rest of the chapter
```

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

The benefits of Effects also benefit initialization.

Using an Effect System for dependencies produces many desirable characteristics at compile time, using plain function calls.
Services are defined as classes with constructor arguments, just as in any vanilla Scala application.

For any given service in your application, you define what it needs in order to execute.
Finally, when it is time to build your application, all of these pieces can be provided in one, flat space.
Each component automatically finds its dependencies, and makes itself available to other components that need it.

Dependency cycles are not allowed by ZIO—you cannot build a program where `A` depends on `B`, and `B` depends on `A`.
You are alerted at compile time about illegal cycles.

ZIO’s dependency management provides capabilities that are not possible in other approaches, such as sharing a single instance of a dependency across multiple test classes, or even multiple applications.

## Let Them Eat Bread

To illustrate how ZIO can assemble our programs, we simulate making and eating `Bread`. [^footnote]
[^footnote]: Although we are utilizing very different tools with different goals, we were inspired by Li Haoyi's excellent article ["What's Functional Programming All About?"](https://www.lihaoyi.com/post/WhatsFunctionalProgrammingAllAbout.html)
Because we will create different types of `Bread`, we start by defining `trait Bread`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

trait Bread:
  def eat =
    printLine:
      "Bread: Eating"
```

This uses `zio.Console.printLine`; as a reminder, calling `eat` just returns an Effect and doesn't display anything on the console until that Effect is run.

We start with the simplest approach of just buying `Bread` from the store:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class BreadStoreBought() extends Bread

object BreadStoreBought:
  val layer =
    ZLayer.succeed:
      BreadStoreBought()
```

In this book, we follow the Scala practice of preferring `case` classes over ordinary classes.
`case` classes are immutable by default and automatically provide commonly-needed functionality.
You aren't required to use `case` classes to work with the Effect system, but they provide valuable conveniences.

The companion object `BreadStoreBought` contains a single method called `layer`.
This produces a special kind of Effect: the `ZLayer`.
`ZLayer`s are used by the Effect System to automatically inject dependencies.
An essential difference between `ZLayer` and other dependency injection systems you might have used is that `ZLayer` validates dependencies *at compile time*.
Your experience will actually be inside your IDE---when you do something problematic your IDE will immediately notify you with a useful error message.
You aren't required to put the function producing a `ZLayer` in a companion object but it is often convenient.

There's something new here: `succeed`.
We need to cheat a little and take some information from the [Failure](05_Failure.md) chapter, which is the next one.
In that chapter, you'll learn that every returned Effect contains information about whether that Effect is successful or has failed.
Each step along the way, that information is checked.
If it fails, the operation is short-circuited and the failure information is produced.
This way you won't have failures randomly propagating through your system, as you do with exceptions.

Sometimes you need to say, "Here's the answer and it's OK."
The `succeed` method produces such an Effect; it is available for both regular `ZIO`s as well as `ZLayers` (There is also a `fail` method to produce a failed Effect).
So `layer` creates a `BreadStoreBought` object and turns it into a successful `ZLayer` Effect.

You can think of a `ZLayer` as a more-powerful constructor.
Like `ZIO` effects, they are deferred, so merely referencing `BreadStoreBought.layer` will not construct anything.
This `ZLayer` provides the `BreadStoreBought` instance as a dependency to any other Effect that needs it.

Now we incorporate the `BreadStoreBought` dependency into a program:

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

`serviceWithZIO` takes a generic parameter, which is the *type* it needs to do the work.
Here, `serviceWithZIO` says, "I need an object that conforms to the `trait Bread`."
The argument to `serviceWithZIO` does the work.
That argument must be a function that returns an Effect---in this case it is the lambda `bread => bread.eat`.
The object it receives from `serviceWithZIO` becomes the `bread` parameter in the lambda.

The whole point of this mechanism is to separate the argument from the function call so that we can make that argument part of the initialization specification.
The initialization specification is the `provide` method,
  which takes one or more `ZLayer` objects and uses them to get the necessary arguments to fill in and complete the code, in this case `bread => bread.eat`.

Although this example is simple enough that you could easily write it without using `ZLayer`, when you start dealing with multiple dependencies, things rapidly become unmanageable.
`ZLayer` keeps everything organized and ensures that all startup dependencies are provided and correct.

### Missing Dependencies

You must provide all required dependencies to an Effect before you can run it.
If the dependency for an Effect isn't provided, you'll get a compile error:

```scala 3 mdoc:fail
ZIO
  .serviceWithZIO[Bread]:
    bread => bread.eat
  .provide()
```

The error tells you exactly what you're missing---and remember that you will see this error in your IDE when you are writing the code.
Traditional dependency injection systems can't tell you until runtime if you're missing something, and even then they typically cannot know for sure if you've covered all your bases.

## Making Bread from Scratch

Instead of buying bread, let's make it from `Dough`, which we will provide as a dependency:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class Dough():
  val letRise =
    printLine:
      "Dough: rising"
```

Note that calling `letRise` produces an Effect.
Dependencies can be anything, including an object that produces an Effect.
```
TODO: is that true (anything)?`
```

Following the pattern of the previous example, a `ZLayer` is produced in the companion object.
This time we create a ZIO object and then convert it using `ZLayer.fromZIO`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

object Dough:
  val fresh =
    ZLayer.fromZIO:
      defer:
        printLine("Dough: Mixed").run
        Dough()
```

In ZIO all managed Effects are contained in ZIO objects, and all Effectful functions return a ZIO object.

Looking at the code from the inside out, the `defer` block executes the `printLine` Effect by calling `.run`, but does *not* call `.run` for `Dough`.
`defer` always produces an Effect, so the result of the `defer` block is an Effect.
The `defer` Effect is passed to `ZLayer.fromZIO` which produces a `ZLayer` object (also an Effect) containing a `Dough` object.

## Multiple Dependencies

Once the `Dough` has risen, we want to bake it. For this we will need some way to apply `Heat`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class Heat()

val oven =
  ZLayer.fromZIO:
    defer:
      printLine("Oven: Heated").run
      Heat()
```

Note that `oven` is a free-standing function in this case; it was not necessary to create it in a companion object.
All you need is some way to produce a `ZLayer`.

We'll make the ability to produce `BreadHomeMade` yet another service, called `homemade`.
That service, in turn, requires two other services, one to produce `Dough` and another that creates `Heat`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class BreadHomeMade(
    heat: Heat,
    dough: Dough
) extends Bread

object Bread:
  val homemade =
    ZLayer.fromZIO:
      defer:
        printLine("BreadHomeMade: Baked").run
        BreadHomeMade(
          ZIO.service[Heat].run,
          ZIO.service[Dough].run
        )
```

`object Bread` is a companion object to `trait Bread`.
The `homemade` method produces a `ZLayer` that itself relies on two other `ZLayer`s, for `Heat` and `Dough`, in order to construct the `BreadHomeMade` object produced by the `homemade` `ZLayer`.
Also note that in the `ZIO.service` calls, we only need to say, "I need `Heat`" and "I need `Dough`" and the Effect System will ensure that those services are found.

The main program starts out looking identical to the previous example: we just need a service that provides `Bread`:

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

But in this case, the `Bread` service is `Bread.homemade`, which itself relies on a source of `Dough` and a source of `Heat`, so we must include all necessary services as arguments to `provide`.
If we don't, the type checker produces helpful error messages (try removing one of the services to see this).

The interrelationships in the `provide` are often called the *dependency graph*.
Here, `Bread.homemade` satisfies the dependency in `serviceWithZIO[Bread]`.
But `Bread.homemade` depends on `Dough.fresh` and `oven`.
You can imagine a tree of dependencies, which is the simplest form of this graph.

In most dependency injection systems, the dependency graph is resolved for you.
This typically happens in some special startup phase of the program which attempts to discover dependencies by following code paths.
Such systems don't always find all dependencies and you don't find out the ones they do discover until runtime.

## Sharing Dependencies

Next, we'd like to start making `Toast`.
Both `Bread` and `Toast` require `Heat`.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class Toast(heat: Heat, bread: Bread):
  val eat =
    printLine:
      "Toast: Eating"

object Toast:
  val make =
    ZLayer.fromZIO:
      defer:
        printLine("Toast: Made").run
        Toast(
          ZIO.service[Heat].run,
          ZIO.service[Bread].run
        )
```

To create `Toast`, we apply some form of `Heat` to some form of `Bread`.
We can use `oven` to provide both forms of `Heat`:

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

The order of the `provide` arguments is unimportant---try reordering them to prove this.

An `oven` is an energy-wasteful way to make `Toast`.
Let's create a dedicated `toaster`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

val toaster =
  ZLayer.fromZIO:
    defer:
      printLine("Toaster: Heated").run
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

This program is ambiguous---it doesn't know whether to make `Toast` in the oven or `Bread` in the toaster.

## Disambiguating Dependencies

To solve the problem, introduce more specific types.
We'll distinguish our `Heat` sources, so they are only used where intended.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class Toaster()

object Toaster:
  val layer =
    ZLayer.fromZIO:
      defer:
        printLine("Toaster: Heating").run
        Toaster()
```

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class ToastZ(
    heat: Toaster,
    bread: Bread
):
  val eat =
    printLine:
      "Toast: Eating"

object ToastZ:
  val make =
    ZLayer.fromZIO:
      defer:
        printLine("ToastZ: Made").run
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
import zio.Console._

val ovenSafe =
  ZLayer.fromZIO:
    ZIO
      .succeed(Heat())
      .tap:
        _ =>
          printLine:
            "Oven: Heated"
      .withFinalizer:
        _ =>
          printLine:
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
import zio.Console._

case class BreadFromFriend() extends Bread()
object Friend:
  def forcedFailure(invocations: Int) =
    defer:
      printLine(s"Attempt $invocations: Failure(Friend Unreachable)").run
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
        printLine(s"Attempt $invocations: Succeeded")
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
      Friend.bread(worksOnAttempt = 3)
```

## Fallback Dependencies

Relying on this method of acquiring `Bread` means we must pay for that convenience.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .service[Bread]
    .provide:
      Friend
        .bread(worksOnAttempt = 3)
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
        .bread(worksOnAttempt = 3)
        .retry:
          Schedule.recurs:
            retries
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  logicWithRetries(retries = 2)
```

## External Configuration

Programs often need to configure their behavior based on the environment during startup.
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

This imports most of the core "Config" datatypes and functions that we need.
We make a case class to hold our values:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class RetryConfig(times: Int)
```

To automatically map values in configuration files to our case class, we import a macro from the `zio.config.magnolia` module:

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

Effects that use external systems are unpredictable.
However, we want to be able to test our programs in a predictable way.
How do we write tests for Effects that are predictable?
We can replace the external systems with predictable ones when running tests.
Rather than trying to get `Bread` from a fallible human, we can create an `IdealFriend` that will always give us `Bread`.

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
This allows us to make assertions on something that is typically a black hole in our code.

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

The `Random` Effect uses an injected something which when running the ZIO uses the system's unpredictable random number generator.
In ZIO Test the `Random` Effect uses a different something which can predictably generate "random" numbers.
`TestRandom` provides a way to define what those numbers are.
This example feeds in the `Int`s `1` and `2` so the first time we ask for a random number we get `1` and the second time we get `2`.

Anything an Effect needs (from the system or the environment) can be substituted in tests for something predictable.
For example, an Effect that fetches users from a database can be simulated with a predictable set of users instead of having to setup a test database with predictable users.

When your program treats randomness as an Effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

### Time

Even time can be simulated, as using the clock is an Effect.

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
Calling a time-based Effect like `timeout` would hang indefinitely with a warning like:

```terminal
Warning: A test is using time, but is not
advancing the test clock, which may result
in the test hanging.  Use TestClock.adjust
to manually advance the time.
```

To test time-based Effects we need to `fork` those Effects so we can adjust the clock.
After adjusting the clock, we can then `join` the Effect where in this case the timeout has then been reached causing the Effect to return a `None`.

Using a simulated Clock means we no longer rely on real-world time.
The example now runs in real-world milliseconds instead of the original 1 second.
Our time-based tests are not based on actual system time so they run much more quickly.
They are also more predictable as the time adjustments are fully controlled by the tests.

#### Targeting Failure-Prone Time Bands

Using real-world time also can be error prone because Effects may have unexpected results in certain time bands.
Suppose you have code that gets the time and it happens to be 23:59:59.
After some operations that take a few seconds, you get database records for the current day.
Those records may no longer be the day associated with previously received records.
This scenario can be very hard to test when using real-world time.
With a simulated clock, your tests can adjust the clock to reliably reproduce the test conditions.
