# Testing Effects

## Testing Unpredictable Effects

Effects need access to external systems thus are unpredictable.  Tests are ideally predictable so how do we write tests for effects that are predictable?  With ZIO we can replace the external systems with predictable ones when running our tests.

With ZIO Test we can use predictable replacements for the standard systems effects (Clock, Random, Console, etc).

## Random

An example of this is Random numbers.  Randomness is inherently unpredictable.  But in ZIO Test, without changing our Effects we can change the underlying systems with something predictable:

```scala mdoc
import zio.test.TestRandom
import zio.test.assertTrue

runSpec:
  defer:
    TestRandom.feedInts(1, 2).run
    val result1 = Random.nextInt.run
    val result2 = Random.nextInt.run
    assertTrue(result1 == 1, result2 == 2)
```

The `Random` Effect uses an injected something which when running the ZIO uses the system's unpredictable random number generator.  In ZIO Test the `Random` Effect uses a different something which can predictably generate "random" numbers.  `TestRandom` provides a way to define what those numbers are.  This example feeds in the `Int`s `1` and `2` so the first time we ask for a random number we get `1` and the second time we get `2`.

Anything an effect needs (from the system or the environment) can be substituted in tests for something predictable.  For example, an effect that fetches users from a database can be simulated with a predictable set of users instead of having to setup a test database with predictable users.

## Time

Even time can be simulated as using the clock is an effect.

```scala mdoc
import zio.test.*

runSpec:
  val thingThatTakesTime = ZIO.sleep(2.seconds)

  defer:
    val fork =
      thingThatTakesTime
        .timeout(1.second)
        .fork
        .run
    TestClock.adjust(2.seconds).run
    val result = fork.join.run
    assertTrue(result.isEmpty)
```

By default in ZIO Test, the clock does not change unless instructed to.  Calling a time based effect like `timeout` would hang indefinitely with a warning like:
```
Warning: A test is using time, but is not advancing the test clock, which may result in the test hanging. Use TestClock.adjust to manually advance the time.
```

To test time based effects we need to `fork` those effects so that then we can adjust the clock.  After adjusting the clock, we can then `join` the effect where in this case the timeout has then been reached causing the effect to return a `None`.

Using a simulated Clock means that we no longer rely on real-world time for time.  So this example runs in milliseconds of real-world time instead of taking an actual 1 second to hit the timeout.  This way our time-based tests run much more quickly since they are not based on actual system time.  They are also more predictable as the time adjustments are fully controlled by the tests.

### Targeting Error-Prone Time Bands

Using real-world time also can be error prone because effects may have unexpected results in certain time bands.  For instance, if you have code that gets the time and it happens to be 23:59:59, then after some operations that take a few seconds, you get some database records for the current day, those records may no longer be the day associated with previously received records.  This scenario can be very hard to test for when using real-world time.  When using a simulated clock in tests, you can write tests that adjust the clock to reliably reproduce the condition.

> Todo: The example could be clarified.

## `assertTrue`

In this example we utilize ZIO Test's `assertTrue` which provides a non-DSL approach to writing assertions while preserving the negative condition error messages.  Typically using `assertTrue` doesn't give helpful errors, ie `true != false`, but ZIO Test provides helpful details for why the assertion was false.

> Todo: Can we display with mdoc the nice assertTrue fail message?
> Todo: More compelling assertTrue failure

```scala mdoc
runSpec:
  assertTrue(Some("asdf") == None)
```


## Test Aspects
We have seen how to add capabilities and behaviors `ZIO`'s enabled by manipulating them as values.
We can add behaviors to `ZSpec`s that are more specific to testing.

### Overriding Builtin Services

### Injecting Behavior before/after/around

### Flakiness
Commonly, as a project grows, the supporting tests become more and more flaky.
This can be caused by a number of factors:

- The code is using shared, live services
  Shared resources, such as a database or a file system, might be altered by other processes.
  These could be other tests in the project, or even unrelated processes running on the same machine.

- The code is not thread safe
  Other processes running simultaneously might alter the expected state of the system.

- Resource limitations
A team of engineers might be able to successfully run the entire test suite on their personal machines.
However, the CI/CD system might not have enough resources to run the tests triggered by everyone pushing to the repository.
Your tests might be occasionally failing due to timeouts or lack of memory.

#### Forbidding
- `nonflaky`

We might have sections of the code that absolutely must be reliable, and we want to express that in our tests.
By using `nonFlaky` we can ensure that the test will fail if it is flaky, by hammering it with repeated executions.
You can dial up the number of iterations to match your reliability expectations.

#### Tolerating/Flagging

In a perfect world, we would fix the underlying issues immediately.
However, under real world constraints, we may need to tolerate flakiness for a time.
ZIO Test provides a few ways to do this.

- `flaky`
This is your goto, easily-applied solution for accommodating legacy flakiness in your codebase.
For the average, undiagnosed "This test fails sometimes" circumstance, this is the right starting point.

- `eventually`
When you have a test that is flaky, but you don't know what a reasonable retry behavior is, use `eventually`.
It's tolerant of any number of failures.


### Platform concerns

### Configuration / Environment
- TestAspect.ifEnv/ifProp

### Time
- Measuring
- Restricting

### What should run?
- Ignore
