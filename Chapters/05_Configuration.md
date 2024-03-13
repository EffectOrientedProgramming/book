# Configuration

1. Application startup uses the same tools that you utilize for the rest of your application

## General/Historic discussion

One reason to modularize an application into "parts" is that the relationship between the parts can be expressed and also changed depending on the needs for a given execution path.  
Typically, this approach to breaking things into parts and expressing what they need, is called "Dependency Injection."

... Why is it called "Dependency Injection" ?
Avoid having to explicitly pass things down the call chain.

There is one way to express dependencies.

Let's consider an example:
We want to write a function that fetches Accounts from a database
The necessary parts might be a `DatabaseService` which provides database connections and a `UserService` which provides the access controls.
By separating these dependencies our from the functionality of fetching accounts, tests can "fake" or "mock" the dependencies to simulate the actual dependency.

In the world of Java these dependent parts are usually expressed through annotations (e.g. `@Autowired` in Spring).
But these approaches are "impure" (require mutability), often rely on runtime magic (e.g. reflection), and require everything that uses the annotations to be created through a Dependency Injection manager, complicating construction flow.  
An alternative to this approach is to use "Constructor Injection" which avoids some of the pitfalls associated with "Field Injection" but doesn't resolve some of the underlying issues, including the ability for dependencies to be expressed at compile time.

If instead functionality expressed its dependencies through the type system, the compiler could verify that the needed parts are in-fact available given a particular path of execution (e.g. main app, test suite one, test suite two).

## What ZIO Provides Us.

With ZIO's approach to dependencies, you get many desirable characteristics at compile-time, using standard language features.
Your services are defined as classes with constructor arguments, just as in any vanilla Scala application.
No annotations that kick off impenetrable wiring logic outside your normal code.

For any given service in your application, you define what it needs in order to execute.
Finally, when it is time to build your application, all of these pieces can be provided in one, flat space.
Each component will automatically find its dependencies, and make itself available to other components that need it.

To aid further in understanding your application architecture, you can visualize the dependency graph with a single line.

You can also do things that simply are not possible in other approaches, such as sharing a single instance of a dependency across multiple test classes, or even multiple applications.

## DI-Wow!
TODO Values to convey:

- Layer Graph
   - Cycles are a compile error
   - Visualization with Mermaid
   - test implementations
- Layer Resourcefulness
   - Layers can have setup & teardown (open & close)

```scala mdoc
// Explain private constructor approach
case class Dough():
  val letRise =
    ZIO.debug:
      "Dough is rising"

object Dough:
  val fresh =
    ZLayer.derive[Dough]
```

## Step 1: Provide Dependency Layers to Effects
We must provide all required dependencies to an effect before you can run it.

```scala mdoc
runDemo:
  ZIO.serviceWithZIO[Dough]:
    dough => dough.letRise
  .provide:
    Dough.fresh
```


## Step 2: Unresolved Dependencies Are Compile Errors

If the dependency for an Effect isn't provided, we get a compile error:

TODO: Decide what to do about the compiler error differences between these approaches

TODO: Can we avoid the `.provide()` and still get a good compile error in mdoc

TODO: Strip `repl.MdocSession.MdocApp.` from output. Remove caret indicator from output.

```scala mdoc:fail
runDemo:
  ZIO.serviceWithZIO[Dough]:
    dough => dough.letRise
  .provide()
```

## Step 3: Dependencies can "automatically" assemble to fulfill the needs of an effect

The requirements for each ZIO operation are tracked and combined automatically.

```scala mdoc:silent
case class Heat()

val oven =
  ZLayer
    .derive[Heat]
```


```scala mdoc
trait Bread

case class BreadHomeMade(
    heat: Heat,
    dough: Dough
) extends Bread

object Bread:
  val homemade =
    ZLayer
      .derive[BreadHomeMade]
```

Something around how like typical DI, the "graph" of dependencies gets resolved "for you"
This typically happens in some completely new/custom phase, that does follow standard code paths.
Dependencies on effects propagate to effects which use effects.

```scala mdoc
runDemo:
  ZIO.service[Bread]
    .provide(
      Bread.homemade,
      Dough.fresh, 
      oven, 
    )
```

## Step 4: Different effects can require the same dependency
Eventually, we grow tired of eating plain `Bread` and decide to start making `Toast`.
Both of these processes require `Heat`.

```scala mdoc
case class Toast(heat: Heat, bread: Bread)

object Toast:
  val make =
    ZLayer
      .derive[Toast]
```

It is possible to also use the oven to provide `Heat` to make the `Toast`.

The dependencies are tracked by their type. 
In this case both `Toast.make` and `Bread.homemade` require `Heat`.

Notice - Even though we provide the same dependencies in this example, oven is _also_ required by `Toast.make`

```scala mdoc
runDemo:
  ZIO.service[Toast]
    .provide(
      Toast.make,
      Bread.homemade,
      Dough.fresh,
      oven
    )
```

However, the oven uses a lot of energy to make `Toast`.
It would be great if we can instead use our dedicated toaster!


```scala mdoc:silent
val toaster =
  ZLayer
    .derive[Heat]
```

```scala mdoc
runDemo:
  ZIO.service[Heat]
    .provide:
      toaster
```

## Step 5: Dependencies must be fulfilled by unique types

```scala mdoc:fail
runDemo:
  ZIO.service[Toast]
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

## Step 6: Providing Dependencies at Different Levels
This enables other effects that use them to provide their own dependencies of the same type

```scala mdoc
runDemo:
  ZIO.serviceWithZIO[Bread]:
    bread =>
      ZIO.service[Toast]
        .provide(
          Toast.make,
          ZLayer.succeed(bread), 
          toaster
        )
  .provide(
    Bread.homemade,
    Dough.fresh,
    oven
  )
```

## Step 7: Effects can Construct Dependencies

```scala mdoc:silent
case class BreadStoreBought() extends Bread

val buyBread =
  ZIO.succeed(BreadStoreBought()).delay(1.second)
```

```scala mdoc:silent
val storeBought =
  ZLayer.fromZIO:
    buyBread
```

```scala mdoc
runDemo:
  ZIO.service[Bread]
  .provide(storeBought)
```


## Step 8: Dependencies can fail
Since dependencies can be built with effects, this means that they can fail.

```scala mdoc:invisible
import zio.Runtime.default.unsafe
case class BreadFromFriend() extends Bread()
object Friend:
  val invocations =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe =
        u
      unsafe
        .run(Ref.make(0))
        .getOrThrowFiberFailure()
    )

  def reset() =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe =
        u
      unsafe
        .run(invocations.set(0))
        .getOrThrowFiberFailure()
    )

  val forcedFailure =
    defer:
      println("Error: **Friend Unreachable**")
      ZIO
        .when(true)(
          ZIO.fail("Error: **Friend Unreachable**")
        )
        .as(???)
        .run
      ZIO.succeed(BreadFromFriend()).run

  def attempt(invocations: Ref[Int]) =
    defer:
      val currentInvocations: Int =
        invocations.updateAndGet(_ + 1).run
      currentInvocations match
        case cnt if cnt < 4 =>
          forcedFailure.run
        case _ =>
          println("Log: Friend answered")
          BreadFromFriend()

// Already constructed elsewhere, that we don't
  // control
  val bread =
    ZLayer.fromZIO:
      Friend.attempt(invocations)
end Friend
```

```scala mdoc:silent
Friend.bread: ZLayer[Any, String, Bread]
```

```scala mdoc
runDemo:
  ZIO.service[Bread]
    .provide:
      Friend.bread
```

## Step 9: Fallback Dependencies

```scala mdoc:silent
Friend.reset()
```

```scala mdoc:silent
val bread =
  Friend
    .bread
      .orElse:
         storeBought
```

```scala mdoc
runDemo:
  ZIO.service[Toast]
  .provide(
    Toast.make,
    bread,
    toaster
  )

```

## Step 10: Dependency Retries

```scala mdoc:silent
Friend.reset()
```

```scala mdoc
def friendBreadWithRetries(times: Int) =
  Friend
    .bread
    .retry:
      Schedule.recurs:
        times
```

```scala mdoc
runDemo: 
  ZIO.service[Bread]
    .provide:
      friendBreadWithRetries:
        1
```

## Step 11: Layer Retry + Fallback?

Maybe retry on the ZLayer eg. (BreadDough.rancid, Heat.brokenFor10Seconds)

```scala mdoc:silent
Friend.reset()
```

```scala mdoc
runDemo:
  ZIO.service[Bread]
    .provide:
      friendBreadWithRetries:
        2
      .orElse:
        storeBought
```

TODO {{ Make like superpowers with vals. Figure out how to add the config without changing a previous step. }}

## Step 12: Externalize Config for Retries

```scala mdoc:silent
Friend.reset()
```

Changing things based on the running environment.

- CLI Params
- Config Files
- Environment Variables

```scala mdoc:silent
import zio.config.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.*

case class RetryConfig(times: Int)

val configDescriptor: Config[RetryConfig] =
  deriveConfig[RetryConfig]

val configProvider =
  ConfigProvider.fromHoconString:
    "{ times: 3 }"

val config =
  ZLayer.fromZIO:
    read:
      configDescriptor.from:
        configProvider
```

```scala mdoc
runDemo:
  ZIO.serviceWithZIO[RetryConfig]:
    retryConfig =>
      ZIO.service[Bread]
        .provide:
          friendBreadWithRetries:
            retryConfig.times
  .provide:
    config
```


## Testing Effects

Effects need access to external systems thus are unpredictable.  
Tests are ideally predictable so how do we write tests for effects that are predictable?
With ZIO we can replace the external systems with predictable ones when running our tests.

With ZIO Test we can use predictable replacements for the standard systems effects (Clock, Random, Console, etc).

## Random

An example of this is Random numbers.  Randomness is inherently unpredictable.  But in ZIO Test, without changing our Effects we can change the underlying systems with something predictable:

```scala mdoc:silent
import zio.test.{
  TestRandom,
  TestAspect,
  assertCompletes
}

val coinToss =
  defer:
    if (Random.nextBoolean.run)
      ZIO
        .debug:
          "R: Heads."
        .run
    else
      ZIO
        .fail:
          "Tails encountered. Ending performance."
        .run

val rosencrantzAndGuildensternAreDead =
  defer:
    TestRandom
      .feedBooleans(Seq.fill(8)(true)*)
      .run
    ZIO
      .debug:
        "*Performance Begins*"
      .run
    coinToss.repeatN(4).run

    ZIO
      .debug:
        "G: There is an art to building suspense."
      .run
    coinToss.run
    ZIO
      .debug:
        "G: Though it can be done by luck alone."
      .run
    coinToss.run
    ZIO
      .debug:
        "G: ...probability"
      .run
    coinToss.run
```

```scala mdoc
runSpec:
  defer:
    rosencrantzAndGuildensternAreDead.run
    assertCompletes
```

```scala mdoc:silent
val spec =
  defer:
    rosencrantzAndGuildensternAreDead.run
    assertCompletes
```

```scala mdoc
// todo: maybe we can change runSpec so that this spec structure matches the previous
runSpec(
  spec,
  TestAspect.withLiveRandom,
  TestAspect.eventually
)
```


The `Random` Effect uses an injected something which when running the ZIO uses the system's unpredictable random number generator.  In ZIO Test the `Random` Effect uses a different something which can predictably generate "random" numbers.  `TestRandom` provides a way to define what those numbers are.  This example feeds in the `Int`s `1` and `2` so the first time we ask for a random number we get `1` and the second time we get `2`.

Anything an effect needs (from the system or the environment) can be substituted in tests for something predictable.  For example, an effect that fetches users from a database can be simulated with a predictable set of users instead of having to setup a test database with predictable users.

When your program treats randomness as an effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

## Time

Even time can be simulated as using the clock is an effect.

```scala mdoc
import zio.test.*

runSpec:
  val slowOperation =
    ZIO.sleep:
      2.seconds

  defer:
    val fork =
      slowOperation
        .timeout:
          1.second
        .fork
        .run
    TestClock
      .adjust:
        2.seconds
      .run
    val result =
      fork.join.run
    assertTrue:
      result.isEmpty
```

By default in ZIO Test, the clock does not change unless instructed to.
Calling a time based effect like `timeout` would hang indefinitely with a warning like:
```
Warning: A test is using time, but is not advancing the test clock, which may result in the test hanging. 
Use TestClock.adjust to manually advance the time.
```

To test time based effects we need to `fork` those effects so that then we can adjust the clock.
After adjusting the clock, we can then `join` the effect where in this case the timeout has then been reached causing the effect to return a `None`.

Using a simulated Clock means that we no longer rely on real-world time for time.
So this example runs in milliseconds of real-world time instead of taking an actual 1 second to hit the timeout.
This way our time-based tests run much more quickly since they are not based on actual system time.
They are also more predictable as the time adjustments are fully controlled by the tests.

### Targeting Error-Prone Time Bands

Using real-world time also can be error prone because effects may have unexpected results in certain time bands.
For instance, if you have code that gets the time and it happens to be 23:59:59, then after some operations that take a few seconds, you get some database records for the current day, those records may no longer be the day associated with previously received records.  This scenario can be very hard to test for when using real-world time.  When using a simulated clock in tests, you can write tests that adjust the clock to reliably reproduce the condition.
