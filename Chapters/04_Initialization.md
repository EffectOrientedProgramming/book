# Initialization

```
// TODO: Rewrite 'Initialization' intro material once I understand it by working through the rest of the chapter
```

Initializing an application from values provided at startup is a perennial challenge in software.
Solutions are diverse, impressive, and often completely bewildering.

One reason to modularize an application into parts is that the relationship between those parts can be adapted for different paths of execution through the program, such as "main app", "test suite one", "test suite two", etc.
Breaking things into parts and expressing what they need is commonly called *Dependency Inversion*.

Dependency Inversion enables *Dependency Injection* which produces more flexible code.
Instead of manually constructing and passing all dependencies through the application, an "Injector" automatically provides instances of those dependencies when they are needed.

Common approaches to implement Dependency Injection rely on runtime inspection (typically using reflection) and require everything to be created through a Dependency Injection manager (the “Injector”).
This complicates construction and can make it difficult or impossible to express dependencies at compile time.

If we instead express dependencies through the type system, the compiler can verify that the needed parts are available given a particular path of execution.

## Effects and Dependencies

Using an Effect System for dependencies produces many desirable compile-time characteristics.
Services are defined as classes with constructor arguments, just as in any vanilla Scala application.
When an application needs startup configuration, you define those configuration elements as services.
When building the application, these services can be provided in a single flat space.
Each component automatically finds its dependencies, and makes itself available to other components that need it.

Dependency cycles are not allowed by ZIO—you cannot build a program where `A` depends on `B`, and `B` depends on `A`.
You are alerted at compile time about illegal cycles.

ZIO’s dependency management provides capabilities that are not possible in other approaches.
For example, you can share a single instance of a dependency across multiple test classes, or even multiple applications.
```
// TODO: Should we demonstrate this?
```

## Let Them Eat Bread

To illustrate how ZIO assembles programs, we simulate making and eating `Bread`.[^1]
[^1]: We were inspired by Li Haoyi's excellent article ["What's Functional Programming All About?"](https://www.lihaoyi.com/post/WhatsFunctionalProgrammingAllAbout.html)
We will create different types of `Bread`, so we start by defining `trait Bread`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

trait Bread:
  def eat = printLine("Bread: Eating")
```

This uses `zio.Console.printLine`, which is an Effect because it modifies an external system.
Calling `eat` just returns an Effect and doesn't display anything on the console until that Effect is run.

We start with the simplest approach of just buying `Bread` from the store:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class BreadStoreBought() extends Bread

object BreadStoreBought:
  val purchased =
    ZLayer.succeed:
      BreadStoreBought()
```

In this book, we follow the Scala practice of preferring `case` classes over ordinary classes.
`case` classes are immutable by default and automatically provide commonly-needed functionality.
You aren't required to use `case` classes to work with the Effect system, but they provide valuable conveniences.

The companion object `BreadStoreBought` contains a single value called `purchased`.
This produces a special kind of Effect: the `ZLayer`.
`ZLayer`s are used by the Effect System to automatically inject dependencies.
An essential difference between `ZLayer` and other dependency injection systems you might have used is that `ZLayer` validates dependencies *at compile time*.
Your experience will actually be inside your IDE---when you do something problematic your IDE will immediately notify you with a useful error message.
You aren't required to put the function producing a `ZLayer` in a companion object, but it is often convenient.

There's something new here: `succeed`.
We need to cheat a little and take some information from the [Failure](05_Failure.md) chapter, which is the next one.
In that chapter, you'll learn that every returned Effect contains information about whether that Effect is successful or has failed.
Each step along the way, that information is checked.
If it fails, the operation is short-circuited and the entire Effect fails.
This way you won't have failures randomly propagating through your system, as you do with exceptions.

Sometimes you need to say, "Here's the answer and it's OK."
The `succeed` method produces such an Effect; it is available for both regular ZIOs and `ZLayers` (There is also a `fail` method to produce a failed Effect).
So `purchased` creates a `BreadStoreBought` object and turns it into a successful `ZLayer` Effect.

You can think of a `ZLayer` as a more-powerful constructor.
Like `ZIO` effects, they are deferred, so merely referencing `BreadStoreBought.purchased` will not construct anything.
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
      BreadStoreBought.purchased
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
If the dependency for an Effect isn't provided, you'll get a compiler error:

```scala 3 mdoc:fail
ZIO
  .serviceWithZIO[Bread]:
    bread => bread.eat
  .provide()
```

The error tells you exactly what you're missing---and remember that you will see this error in your IDE when you are writing the code.
Traditional dependency injection systems can't tell you until runtime if you're missing something, and even then they typically cannot know for sure if you've covered all your bases.

### Names

An Effect is something that produces an action, so we name ZIOs with verbs.
The `ZLayer` is the structure that provides an instance to your application.
This instance is a thing.
Thus, the net effect of the name of a `ZLayer` should be a noun.
We say "net effect" here because, inside the companion object, we typically use an adjective for the method that produces the `ZLayer`.
However, the name of the companion object *together* with the adjective still produces a noun.

Here, `makeX` is a verb describing the act of creation:

```scala 3 mdoc:silent
import zio.*
import zio.Console.*
import zio.direct.*

case class X():
  val display = printLine("X.display")

val makeX =
  defer:
    printLine("Creating X").run
    X()
```

As a standalone function, the `ZLayer` name should be a noun:

```scala 3 mdoc:silent
import zio.*

val dependency =
  ZLayer.fromZIO:
    makeX
```

Inside the companion object, we can use an adjective, because in order to reference the `ZLayer` you must include the class name, producing the noun `X.dependent`:

```scala 3 mdoc:silent
import zio.*

object X:
  val dependent =
    ZLayer.fromZIO:
      makeX
```

We declare `X.dependent` a noun in the world of programming where we mash words together.
Now we provide the dependency in a program:

```scala 3 mdoc:runzio
import zio.*

def run =
  ZIO
    .serviceWithZIO[X]:
      x => x.display
    .provide:
      X.dependent   // The "adjectivized object"
      // dependency // Or the noun version
```

`serviceWithZIO` needs an `X` object that it substitutes as `x` in the lambda.
This is provided by either `X.dependent` or `dependency`, which yields a `ZLayer` holding the object produced by `make`.
The `ZLayer` is the holder for the object produced by `make`, and it provides a way to get to that object.

```
// TODO: Better names than `dependent` and `dependency`?
```

### Initialization Steps

Adding trace information to the previous example reveals the steps of program initialization:

```scala 3 mdoc:silent
case class Y():
  val display = printLine("Y.display")

val makeY =
  defer:
    printLine("makeY.run creating Y()").run
    Y()

object Y:
  val dependency =
    ZLayer.fromZIO:
      makeY
```

In the main program we capture and display the intermediate values:

```scala 3 mdoc:invisible
def _type(obj: Any): String =
  obj.getClass.getName.split("\\$")(0)

def showType(id: String, obj: Any) =
  printLine(s"$id is a ${_type(obj)}")
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    showType("makeY", makeY).run
    val r = makeY.run
    printLine(s"makeY.run returned $r").run
    showType("Y.dependency", Y.dependency).run

    val main =
      ZIO.serviceWithZIO[Y]:
        y =>
          defer:
            printLine(s"y: $y").run
            y.display.run
      .provide:
        Y.dependency

    showType("main", main).run
    main.run
    printLine("main.run complete").run
```

`showType` is a hidden function producing a ZIO that displays type information for an object.
`makeY` produces a ZIO, which is an un-executed program.
We execute it with `.run` and capture the result in `r`.
The result in `r` is a `Y` object, the one we return at the end of `makeY`.

`Y.dependency` produces a `ZLayer`.
We use that `ZLayer` in the `provide` inside `main`, where it produces the required `Y` object.
`main` is also a ZIO.
When we call `main.run`, `makeY` is called to produce the necessary `Y`.

## Making Bread from Scratch

Instead of buying bread, let's make it from `Dough`, which we will provide as a dependency:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class Dough():
  val letRise = printLine("Dough: rising")
```

Note that calling `letRise` produces an Effect.
Dependencies can be anything (except `Nothing`), including Effects, primitives, custom types, unions, etc.

Following the pattern of previous examples, a `ZLayer` is produced in the companion object.
This time we create a ZIO object using `defer` and then convert it using `ZLayer.fromZIO`:

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
`defer` always produces an Effect, so the result of the `defer` block is an Effect that produces `Dough`.
The `defer` Effect is passed to `ZLayer.fromZIO` which produces a `ZLayer` object (also an Effect) containing a `Dough` object.

## Multiple Dependencies

Once the `Dough` has risen, we want to bake it.
For this we need some kind of `HeatSource`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

trait HeatSource
case class Oven() extends HeatSource

object Oven:
  val heated =
    ZLayer.fromZIO:
      defer:
        printLine("Oven: Heated").run
        Oven()
```

`Oven.heated` is a `ZLayer` that produces an `Oven` object.

In the following, `baked` produces `BreadHomeMade`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class BreadHomeMade(
  heat: HeatSource,
  dough: Dough
) extends Bread

object BreadHomeMade:
  val baked =
    ZLayer.fromZIO:
      defer:
        printLine("BreadHomeMade: Baked").run
        BreadHomeMade(
          ZIO.service[Oven].run,
          ZIO.service[Dough].run
        )
```

The `baked` method is a `ZLayer` which itself relies on two other `ZLayer`s, for `HeatSource` and `Dough`.
With these it constructs the `BreadHomeMade` object produced by the `baked` `ZLayer`.
In the `ZIO.service` calls, we only need to say, "I need an `Oven`" and "I need `Dough`" and the Effect System ensures those services are found.

Initially, the `run` looks identical to the previous example---we just need a service that provides `Bread`:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[Bread]:
      bread => bread.eat
    .provide(
      BreadHomeMade.baked,
      Dough.fresh,
      Oven.heated
    )
```

In this case, the `Bread` service is `BreadHomeMade.baked`, which needs `Dough` and a `HeatSource`, so we must include all necessary services as arguments to `provide`.
If we don't, the type checker produces helpful error messages (try removing one of the services to see this).

The interrelationships in `provide` are often called the *dependency graph*.
Here, `BreadHomeMade.baked` satisfies the dependency in `serviceWithZIO[Bread]`.
But `BreadHomeMade.baked` needs the services `Dough.fresh` and `Oven.heated`.
You can imagine a tree of dependencies, which is the simplest form of this graph.

In most dependency injection systems, the dependency graph is resolved for you.
This typically happens in some special startup phase of the program that attempts to discover dependencies by following code paths.
Such systems don't always find all dependencies, and you don't see the ones they find until runtime.

## Sharing Dependencies

Next, we'd like to make `Toast`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

trait Toast:
  def bread: Bread
  def heat: HeatSource
  val eat = printLine("Toast: Eating")

case class ToastA(heat: HeatSource, bread: Bread) extends Toast

object ToastA:
  val toasted =
    ZLayer.fromZIO:
      defer:
        printLine("ToastA: Made").run
        ToastA(
          ZIO.service[HeatSource].run,
          ZIO.service[Bread].run
        )
```

Both `Bread` and `Toast` require a `HeatSource`.
A `Toaster` is a `HeatSource`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class Toaster() extends HeatSource

object Toaster:
  val ready =
    ZLayer.fromZIO:
      defer:
        printLine("Toaster: Ready").run
        Toaster()
```

Now we have all the services necessary for `Toast`:

```scala 3 mdoc:fail
import zio.*

def run =
  ZIO
    .service[Toast]
    .provide(
      ToastA.toasted,
      Dough.fresh,
      BreadHomeMade.baked,
      Oven.heated,
      Toaster.ready,
    )
```

Because both `Oven` and `Toaster` are `HeatSource`s, trying to include `Toaster.ready` produces an ambiguity error.
If we comment the `Toaster.ready` line, the program uses the `Oven` for both. 

### Disambiguating Dependencies

To solve the problem, introduce more specific types.
We create a type of `Toast` that requires a `Toaster` rather than just any `HeatSource`:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class ToastB(heat: Toaster, bread: Bread) extends Toast
// ToastA used HeatSource for heat

object ToastB:
  val toasted =
    ZLayer.fromZIO:
      defer:
        printLine("ToastB: Made").run
        ToastB(
          ZIO.service[Toaster].run,
          ZIO.service[Bread].run
        )
```

Now we only use the `Oven` to bake the `Bread` and the `Toaster` to make `Toast`:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[Toast]:
      toast => toast.eat
    .provide(
      ToastB.toasted,
      Dough.fresh,
      BreadHomeMade.baked,
      // The two HeatSources don't clash:
      Oven.heated,
      Toaster.ready,
    )
```

The order of the `provide` arguments is unimportant---try reordering them to prove this.

We can create a *wiring graph*:

```terminal
[info]   ZLayer Wiring Graph
[info] ◉ ToastB.toasted
[info] ├─◑ Toaster.ready
[info] ╰─◑ BreadHomeMade.baked
[info]   ├─◑ Oven.heated
[info]   ╰─◑ Dough.fresh
```

`ToastB` requires a `Toaster` and `Bread` (try rewriting the example to use `BreadStoreBought`).
To provide `BreadHomeMade`, we need `Dough` and an `Oven`.

## Dependency Cleanup

An Effect without outstanding dependencies can be used to construct a `ZLayer`.
We can use this to correct a dangerous oversight: We heat up our `Oven`, but never turn it off!
We can build an `Oven` that turns itself off when it is no longer needed.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*
import zio.Console._

case class OvenSafe() extends HeatSource

object OvenSafe:
  val heated =
    ZLayer.fromZIO:
      ZIO
        .succeed(HeatSource())
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

```
// TODO: Only time .tap is used visibly in the book. Intended? 
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  ZIO
    .serviceWithZIO[Bread]:
      bread => bread.eat
    .provide(
      BreadHomeMade.baked,
      Dough.fresh,
      OvenSafe.heated,
      Scope.default
    )
```

```
// TODO: First occurrence of Scope.default
```


## Construction Failure

Since dependencies can be built with Effects, this means that they can fail.
Suppose we have a `Friend` who will sometimes give us `Bread`, but not right away:

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*
import zio.Console._

case class BreadFromFriend() extends Bread()

object Friend:
  def forcedFailure(invocations: Int) =
    defer:
      printLine(
        s"Attempt $invocations: Failure(Friend Unreachable)"
      ).run
      ZIO
        .when(true)(
          ZIO.fail(
            "Failure(Friend Unreachable)"
          )
        )
        .as(???)
        .run
      ZIO.succeed(BreadFromFriend()).run

  def bread(worksOnAttempt: Int) =
    var invocations = 0
    ZLayer.fromZIO:
      invocations += 1
      if invocations < worksOnAttempt then
        forcedFailure(invocations)
      else if invocations == 1 then
        ZIO.succeed(BreadFromFriend())
      else
        printLine(
          s"Attempt $invocations: Succeeded"
        ).orDie
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

If we keep asking, we eventually get `Bread`.

### Fallback Dependencies

If our `Friend` doesn't have `Bread` to give us, we can set up a fallback strategy:

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
          BreadStoreBought.purchased
```

If we can't get `Bread` from our `Friend`, we go to the store and buy it.

### Retries

We can add a `retry` to the `ZLayer` produced by `Friend.bread`:

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  val retries = 2
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

Extension operations like `retry` also work on `ZLayer`s!

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

This imports most of the core "Config" datatypes and functions we need.
We make a case class to hold our values:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

case class RetryConfig(times: Int)
```

To automatically map values from configuration files to our case class, we the `deriveConfig` macro from the `zio.config.magnolia` module:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

import zio.config.magnolia.deriveConfig

val configDescriptor: Config[RetryConfig] =
  deriveConfig[RetryConfig]
```
```
// TODO: Is the type annotation required here because of the macro?
```


The library is modularized, so you only import the tools you need.
To use the Typesafe configuration format, we import everything from that module:

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

```
// TODO where did logicWithRetries come from?
```


Now we have bridged the gap between our logic and configuration files.
This was a longer detour than our other steps, but a common requirement in real-world applications.

## Test Dependencies
```
// TODO: This really feels like a separate chapter, even if it ends up being the shortest.
```


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
```
// TODO: This is the first mention. Was it to be introduced earlier? Also seems like it should be zio.test not zio-test
```

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

Finally, we assign it to the `spec` field of a test class.
In real test code, you would be using `ZIOSpecDefault`, or one of the other `ZIOSpec*` variants. We use our custom test runner here for brevity.

```scala 3 mdoc:testzio
import zio.*
import zio.direct.*

def spec =
  testCase
```

Historically, when call we call `println`, its output disappears into the void.
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
For example, an Effect that fetches users from a database can be simulated with a predictable set of users instead of having to set up a test database with predictable users.

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

By default, in ZIO Test, the clock does not change unless instructed to.
Calling a time-based Effect like `timeout` would hang indefinitely with a warning like:

```terminal
Warning: A test is using time, but is not
advancing the test clock, which may result
in the test hanging.  Use TestClock.adjust
to manually advance the time.
```

We need to explicitly advance the time to make the test complete.

```scala 3 mdoc:silent testzio
import zio.test.*

val timeTravel =
  TestClock.adjust:
    24.hours
```

However, be aware that it is not correct to call `TestClock.adjust` before or after we execute `nightlyBatch`.
We need to adjust the clock *while `nightlyBatch` is running*.

```scala 3 mdoc:testzio
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
