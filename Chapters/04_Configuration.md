# Configuration

Altering the behavior of your application based on values provided at runtime is a perennial challenge in software.
The techniques for solving this problem are diverse, impressive, and often completely bewildering.

## General/Historic discussion

One reason to modularize an application into "parts" is that the relationship between the parts can be expressed and also changed depending on the needs for a given execution path.  
Typically, this approach to breaking things into parts and expressing what they need, is called "Dependency Inversion."

By following "Dependency Inversion", you enable "Dependency Injection", which produces more flexible code.

Instead of manually constructing and passing all your dependencies through the application, you have an "Injector" that automatically provides instances where needed.

Understanding these terms is not crucial for writing Effect Oriented code, but will help when building the layers in your application.

In the world of Java these dependent parts are usually expressed through annotations (e.g. `@Autowired` in Spring).
But these approaches often rely on runtime magic (e.g. reflection) and require everything to be created through a Dependency Injection manager, complicating construction flow.  
Importantly, it is difficult or impossible to express our dependencies at compile time.

Instead, if functionality expressed its dependencies through the regular type system, the compiler could verify that the needed parts are available given a particular path of execution (e.g. main app, test suite one, test suite two).

## What ZIO Provides Us

1. Application startup uses the same tools that you utilize for the rest of your application

With ZIO's approach to dependencies, you get many desirable characteristics at compile-time, using standard language features.
Your services are defined as classes with constructor arguments, just as in any vanilla Scala application.
No annotations that kick off impenetrable wiring logic outside your normal code.

For any given service in your application, you define what it needs in order to execute.
Finally, when it is time to build your application, all of these pieces can be provided in one, flat space.
Each component will automatically find its dependencies, and make itself available to other components that need it.

To aid further in understanding your application architecture, you can visualize the dependency graph with a single line.

You can also do things that simply are not possible in other approaches, such as sharing a single instance of a dependency across multiple test classes, or even multiple applications.

## DI-Wow

TODO Values to convey:

- Layer Graph
  - Cycles are a compile error
  - Visualization with Mermaid

```scala 3 mdoc:silent
// Explain private constructor approach
case class Dough():
  val letRise =
    Console.printLine:
      "Dough is rising"

object Dough:
  val fresh =
    ZLayer.derive[Dough]
      .tap(_ => Console.printLine("Dough: Mixed"))
```

## Step 1: Provide Dependencies to Effects

We must provide all required dependencies to an effect before you can run it.

```scala 3 mdoc:runzio
def run =
  ZIO
    .serviceWithZIO[Dough]:
      dough => dough.letRise
    .provide:
      Dough.fresh
```

## Step 2: Missing Dependencies Are Compile Errors

If the dependency for an Effect isn't provided, we get a compile error:

TODO: Decide what to do about the compiler error differences between these approaches

TODO: Can we avoid the `.provide()` and still get a good compile error in mdoc

TODO: Strip `repl.MdocSession.MdocApp.` from output. Remove caret indicator from output.

```scala 3 mdoc:fail
ZIO
  .serviceWithZIO[Dough]:
    dough => dough.letRise
  .provide()
```

## Step 3: Dependencies can "automatically" assemble

The requirements for each ZIO operation are tracked and combined automatically.

```scala 3 mdoc:silent
case class Heat()

// TODO Version of oven that turns off when finished?
val oven =
  ZLayer.derive[Heat]
    .tap(_ => Console.printLine("Oven: Heated"))
    
```

```scala 3 mdoc:silent
trait Bread {
  def eat =
    Console.printLine("Bread: Eating")
}

case class BreadHomeMade(
    heat: Heat,
    dough: Dough
) extends Bread

object Bread:
  val homemade =
    ZLayer.derive[BreadHomeMade]
      .tap(_ => Console.printLine("BreadHomeMade: Baked"))
```

Something around how like typical DI, the "graph" of dependencies gets resolved "for you"
This typically happens in some completely new/custom phase, that does follow standard code paths.
Dependencies on effects propagate to effects which use effects.

```scala 3 mdoc:runzio
def run =
  ZIO
    .serviceWithZIO[Bread]:
      bread => bread.eat
    .provide(Bread.homemade, Dough.fresh, oven)
```

## Step 4: Different effects can require the same dependency

Eventually, we grow tired of eating plain `Bread` and decide to start making `Toast`.
Both of these processes require `Heat`.

```scala 3 mdoc:silent
case class Toast(heat: Heat, bread: Bread):
  val eat =
    Console.printLine("Toast: Eating")

object Toast:
  val make =
    ZLayer.derive[Toast]
      .tap(_ => Console.printLine("Toast: Made"))
```

It is possible to also use the oven to provide `Heat` to make the `Toast`.

The dependencies are tracked by their type.
In this case both `Toast.make` and `Bread.homemade` require `Heat`.

Notice - Even though we provide the same dependencies in this example, oven is _also_ required by `Toast.make`

```scala 3 mdoc:runzio
def run =
  ZIO
    .service[Toast]
    .provide(
      Toast.make,
      Bread.homemade,
      Dough.fresh,
      oven,
    )
```


However, the oven uses a lot of energy to make `Toast`.
It would be great if we can instead use our dedicated toaster!

```scala 3 mdoc:silent
val toaster =
  ZLayer.derive[Heat]
   .tap(_ => Console.printLine("Toaster: Heated"))
```

```scala 3 mdoc:runzio
def run =
  ZIO
    .service[Heat]
    .provide:
      toaster
```

## Step 5: Dependencies must be unique types

```scala 3 mdoc:fail
ZIO
  .service[Toast]
  .provide(
    Toast.make,
    Dough.fresh,
    Bread.homemade,
    oven,
    toaster,
  )
```

Unfortunately our program is now ambiguous.
It cannot decide if we should be making `Toast` in the oven, `Bread` in the toaster, or any other combination.

## Step 6: Can Disambiguate Dependencies When Needed

TODO Consider: Instead of providing at different levels, show that using _introducing_ a more specific type is usually the better approach. 
I think this will be a big improvement. We can keep everything nice and flat that way.

```scala 3 mdoc:silent
case class Toaster()
object Toaster:
  val layer =
    ZLayer.derive[Toaster]
      .tap(_ => Console.printLine("Toaster: Heating"))
```

```scala 3 mdoc:silent
case class ToastZ(heat: Toaster, bread: Bread):
  val eat =
    Console.printLine("Toast: Eating")

object ToastZ:
  val make =
    ZLayer.derive[ToastZ]
      .tap(_ => Console.printLine("ToastZ: Made"))
```

We can explicitly provide dependencies when needed, to prevent ambiguity.

```scala 3 mdoc:runzio
def run =
  ZIO
    .serviceWithZIO[ToastZ]:
      toast => toast.eat
    .provide(
      ToastZ.make,
      Toaster.layer,
      Bread.homemade, 
      Dough.fresh, 
      oven,
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

## Step 7: Effects can Construct Dependencies

So far, we have focused on providing `Layer`s to Effects, but this can also go the other way!
If an Effect already has no outstanding dependencies, it can be used to construct a `Layer`.

```scala 3 mdoc:silent
case class BreadStoreBought() extends Bread

val buyBread =
  ZIO.succeed:
    BreadStoreBought()
```

```scala 3 mdoc:silent
val storeBought =
  ZLayer.fromZIO:
    buyBread
  .tap(_ => Console.printLine("BreadStoreBought: Bought"))
```

```scala 3 mdoc:runzio
def run =
  ZIO
    .service[Bread]
    .provide:
      storeBought
```

## Step 8: Dependency construction can fail

Since dependencies can be built with effects, this means that they can fail.

```scala 3 mdoc:invisible
case class BreadFromFriend() extends Bread()
object Friend:
  def forcedFailure(invocations: Int) =
    defer:
      Console
        .printLine(
          s"Attempt $invocations: Error(Friend Unreachable)"
        )
        .run
      ZIO
        .when(true)(
          ZIO.fail("Error(Friend Unreachable)")
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
def run =
  ZIO
    .service[Bread]
    .provide:
      Friend.bread(worksOnAttempt =
        3
      )
```

## Step 9: Fallback Dependencies

```scala 3 mdoc:runzio
def run =
  ZIO
    .service[Bread]
    .provide:
      Friend
        .bread(worksOnAttempt =
          3
        )
        .orElse:
          storeBought
```

## Step 10: Dependency Retries

```scala 3 mdoc
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
def run =
  logicWithRetries(retries = 1)
```

```scala 3 mdoc:runzio
def run =
  logicWithRetries(retries = 2)
```

## Step 11: Externalize Config for Retries

Changing things based on the running environment.

- CLI Params
- Config Files
- Environment Variables

We can use the ZIO Config library to manage these.
This is one of the few additional libraries that we use on top of core ZIO.
It is heavily modularized so that you only pull in the integrations for the technologies used in your project.

```scala 3 mdoc:silent
import zio.config.*
```

This import brings in most of the core Config datatypes and functions that we need.
Next we make a case class that will hold our values.

```scala 3 mdoc:silent
case class RetryConfig(times: Int)
```

To automatically map values in config files to our case class, we import a macro from the zio-config "magnolia" module.

```scala 3 mdoc:silent

import zio.config.magnolia.deriveConfig

val configDescriptor: Config[RetryConfig] =
  deriveConfig[RetryConfig]
```

We want to use the Typesafe config format, so we import everything from that module.

```scala 3 mdoc:silent
import zio.config.typesafe.*
```

```scala 3 mdoc:silent
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
def run =
  ZIO
    .serviceWithZIO[RetryConfig]:
      retryConfig =>
        logicWithRetries(
          retries = retryConfig.times
        )
    .provide:
      config
```

## Step 12: Keep the building from burning down!

TODO Figure out best order. Might be better closer to when Step 7 (Effects can construct dependencies)

Throughout our kitchen scenarios, there has been a dangerous oversight. 
We heat up our oven, but then never turn it off!
It would be great to have an oven that automatically turns itself off when we are done using it.

```scala 3 mdoc:silent
// TODO Split this up? It's pretty busy.
// TODO Can we introduce acquireRelease in isolation in superpowers?
val ovenSafe =
  ZLayer.fromZIO:
    ZIO.succeed(Heat())
      .tap(_ => Console.printLine("Oven: Heated"))
      .withFinalizer(_ => Console.printLine("Oven: Turning off!").orDie)
```


```scala 3 mdoc:runzio
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

## Testing Effects

TODO: Bridge from dependency / configuration to here

TODO: Code that provides an "ideal friend" to our bread example
    Maybe as a 2 step process, first outside of a test, then in a test.
    Or a single step just in a test

Effects need access to external systems thus are unpredictable.  
Tests are ideally predictable so how do we write tests for effects that are predictable?
With ZIO we can replace the external systems with predictable ones when running our tests.

With ZIO Test we can use predictable replacements for the standard systems effects (Clock, Random, Console, etc).

### Random

An example of this is Random numbers.  Randomness is inherently unpredictable.  But in ZIO Test, without changing our Effects we can change the underlying systems with something predictable:

```scala 3 mdoc:silent
val coinToss =
  // TODO: This is the first place we use defer.
  // We need to deliberately, and explicitly,
  // introduce it.
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

```scala 3 mdoc:silent
val rosencrantzCoinToss =
  coinToss.debug:
    "R"

val rosencrantzAndGuildensternAreDead =
  defer:
    ZIO
      .debug:
        "*Performance Begins*"
      .run
    rosencrantzCoinToss.repeatN(4).run

    ZIO
      .debug:
        "G: There is an art to building suspense."
      .run
    rosencrantzCoinToss.run

    ZIO
      .debug:
        "G: Though it can be done by luck alone."
      .run
    rosencrantzCoinToss.run

    ZIO
      .debug:
        "G: ...probability"
      .run
    rosencrantzCoinToss.run
```

```scala 3 mdoc:testzio
import zio.test.*

def spec =
  test(
    "rosencrantzAndGuildensternAreDead finishes"
  ):
    defer:
      TestRandom
        .feedBooleans:
          true
        .repeatN:
          7
        .run
      rosencrantzAndGuildensternAreDead.run
      assertCompletes
```

```scala 3 mdoc:testzio
import zio.test.*

def spec =
  test("flaky plan"):
    defer:
      rosencrantzAndGuildensternAreDead.run
      assertCompletes
  @@ TestAspect.withLiveRandom @@
    TestAspect.flaky(Int.MaxValue)
```

The `Random` Effect uses an injected something which when running the ZIO uses the system's unpredictable random number generator.  In ZIO Test the `Random` Effect uses a different something which can predictably generate "random" numbers.  `TestRandom` provides a way to define what those numbers are.  This example feeds in the `Int`s `1` and `2` so the first time we ask for a random number we get `1` and the second time we get `2`.

Anything an effect needs (from the system or the environment) can be substituted in tests for something predictable.  For example, an effect that fetches users from a database can be simulated with a predictable set of users instead of having to setup a test database with predictable users.

When your program treats randomness as an effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

### Time

Even time can be simulated as using the clock is an effect.

```scala 3 mdoc:silent
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
      val fork =
        nightlyBatch.fork.run
      timeTravel.run
      fork.join.run

      assertCompletes
```

The `race` is between `nightlyBatch` and `timeTravel`.
It completes when the first Effect succeeds and cancels the losing Effect, using the Effect System's cancellation mechanism.

By default in ZIO Test, the clock does not change unless instructed to.
Calling a time based effect like `timeout` would hang indefinitely with a warning like:

```terminal
Warning: A test is using time, but is not 
advancing the test clock, which may result 
in the test hanging.  Use TestClock.adjust 
to manually advance the time.
```

To test time based effects we need to `fork` those effects so that then we can adjust the clock.
After adjusting the clock, we can then `join` the effect where in this case the timeout has then been reached causing the effect to return a `None`.

Using a simulated Clock means that we no longer rely on real-world time for time.
So this example runs in milliseconds of real-world time instead of taking an actual 1 second to hit the timeout.
This way our time-based tests run much more quickly since they are not based on actual system time.
They are also more predictable as the time adjustments are fully controlled by the tests.

#### Targeting Error-Prone Time Bands

Using real-world time also can be error prone because effects may have unexpected results in certain time bands.
For instance, if you have code that gets the time and it happens to be 23:59:59, then after some operations that take a few seconds, you get some database records for the current day, those records may no longer be the day associated with previously received records.  This scenario can be very hard to test for when using real-world time.  When using a simulated clock in tests, you can write tests that adjust the clock to reliably reproduce the condition.
