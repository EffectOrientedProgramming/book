# Testing Effects

TODO: Combine with "Configuration" ?

Effects need access to external systems thus are unpredictable.  
Tests are ideally predictable so how do we write tests for effects that are predictable?
With ZIO we can replace the external systems with predictable ones when running our tests.

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

When your program treats randomness as an effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

```scala mdoc:silent
TestRandom.feedBooleans(true, false)
TestRandom.feedBytes(Chunk(1, 2, 3))
TestRandom.feedChars('a', 'b', 'c')
TestRandom.feedDoubles(1.0, 2.0, 3.0)
TestRandom.feedFloats(1.0f, 2.0f, 3.0f)
TestRandom.feedInts(1, 2, 3)
TestRandom.feedLongs(1L, 2L, 3L)
TestRandom.feedStrings("a", "b", "c")
TestRandom.feedUUIDs(
  java
    .util
    .UUID
    .fromString(
      "00000000-0000-0000-0000-000000000001"
    )
)
```

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
    val result = fork.join.run
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

Using real-world time also can be error prone because effects may have unexpected results in certain time bands.  For instance, if you have code that gets the time and it happens to be 23:59:59, then after some operations that take a few seconds, you get some database records for the current day, those records may no longer be the day associated with previously received records.  This scenario can be very hard to test for when using real-world time.  When using a simulated clock in tests, you can write tests that adjust the clock to reliably reproduce the condition.

> Todo: The example could be clarified.

## `assertTrue`

In this example we utilize ZIO Test's `assertTrue` which provides a non-DSL approach to writing assertions while preserving the negative condition error messages.  Typically using `assertTrue` doesn't give helpful errors, ie `true != false`, but ZIO Test provides helpful details for why the assertion was false.

> Todo: Can we display with mdoc the nice assertTrue fail message?
> Todo: More compelling assertTrue failure

```scala mdoc
runSpec:
  assertTrue:
    Some("asdf") == None
```


## Test Aspects
We have seen how to add capabilities and behaviors `ZIO`'s enabled by manipulating them as values.
We can add behaviors to `ZSpec`s that are more specific to testing.

### Overriding Builtin Services
When testing `ZIO`s we can provide user-defined Environment types by using `.provide`.
However, the Built-in Services are not part of the Environment, so we need a different way to override them.
By default, tests will get `Test` versions of the Built-in Services.

```scala mdoc
runSpec:

  defer:
    val slowOperation = ZIO.sleep(2.seconds)
    val result =
      defer:
        val fork = slowOperation.fork.run
        TestClock.adjust(10.seconds).run
        fork.join.run
      .timed
        .run
    println(result)
    assertCompletes
```
```scala mdoc
runSpec(
  defer:
    val slowOperation = ZIO.sleep(2.seconds)
    val result =
      defer:
        val fork = slowOperation.fork.run
        TestClock.adjust(10.seconds).run
        fork.join.run
      .timed
        .run
    println(result)
    assertCompletes
  ,
  TestAspect.withLiveClock
)
```

### Injecting Behavior before/after/around

```scala mdoc
runSpec(
  defer:
    println("During test")
    assertCompletes
  ,
  TestAspect.around(
    ZIO.debug:
      "ZIO IO, before"
    ,
    ZIO.succeed:
      println("plain IO, after")
  )
)
```

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

```scala mdoc
runSpec(
  defer:
    assertTrue:
      Random.nextBoolean.run
  ,
  TestAspect.withLiveRandom,
  TestAspect.flaky
)
```


#### Forbidding
- `nonflaky`

We might have sections of the code that absolutely must be reliable, and we want to express that in our tests.
By using `nonFlaky` we can ensure that the test will fail if it is flaky, by hammering it with repeated executions.
You can dial up the number of iterations to match your reliability expectations.


```scala mdoc
runSpec(
  defer:
    assertTrue:
      Random.nextInt.run != 42
  ,
  TestAspect.withLiveRandom,
  TestAspect.nonFlaky
)
```

#### Tolerating/Flagging

In a perfect world, we would fix the underlying issues immediately.
However, under real world constraints, we may need to tolerate flakiness for a time.
ZIO Test provides a few ways to do this.

- `flaky`
This is your goto, easily-applied solution for accommodating legacy flakiness in your codebase.
For the average, undiagnosed "This test fails sometimes" circumstance, this is the right starting point.

- `eventually`
When you have a test that is flaky, but you don't know what a reasonable retry behavior is, use `eventually`.
It's tolerant of any number of failures, and will just keep retrying until interrupted by other mechanisms.


### Platform concerns

### Configuration / Environment
- TestAspect.ifEnv/ifProp

### Time
#### Measuring Time
Since there is already a `.timed` method available directly on `ZIO` instances, it might seem redundant to have a `timed` `TestAspect`.
However, they are distinct enough to justify their existence.
`ZIO`s `.timed` methods changes the result type of your code by adding the duration to a tuple in the result.
This is useful, but requires the calling code to handle this new result type.
`TestAspect.timed` is a non-invasive way to measure the duration of a test.
The timing information will be managed behind the scenes, and printed in the test output, without changing any other behavior.

#### Restricting Time
Sometimes, it's not enough to simply track the time that a test takes.
If you have specific Service Level Agreements (SLAs) that you need to meet, you want your tests to help ensure that you are meeting them. 
However, even if you don't have contracts bearing down on you, there are still good reasons to ensure that your tests complete in a timely manner.
Services like GitHub Actions will automatically cancel your build if it takes too long, but this only happens at a very coarse level.
It simply kills the job and won't actually help you find the specific test responsible.

A common technique is to define a base test class for your project that all of your tests extend.
In this class, you can set a default upper limit on test duration.
When a test violates this limit, it will fail with a helpful error message.

This helps you to identify tests that have completely locked up, or are taking an unreasonable amount of time to complete.

For example, if you are running your tests in a CI/CD pipeline, you want to ensure that your tests complete quickly, so that you can get feedback as soon as possible.
you can use `TestAspect.timeout` to ensure that your tests complete within a certain time frame.

### What should run?
It would be great if all our tests could run & pass at every moment in time, but there are times when it's not feasible.
If you are doing Test-Driven Development, you don't want the build to be broken until you are completely finished implementing the feature.
If you are rewriting a significant part of your project, you already know there are going to be test failures until you are finished.
Traditionally, we comment out the tests in these situations.
However, this can lead to a lot of noise in the codebase, and it's easy to forget to uncomment the tests when you are done.
`TestAspect`s provide a better way to handle this.

```scala mdoc
runSpec(
  defer:
    assertNever:
      "Not implemented. Do not run"
)
```

```scala mdoc
runSpec(
  defer:
    assertNever:
      "Not implemented. Do not run"
  ,
  TestAspect.ignore
)
```

## Defining a base test class for your project
