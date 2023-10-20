# Dependency Injection

1. Application startup uses the same tools that you utilize for the rest of your application
1. App vs Test
1. Composability

## Bill

Cognira - Distributed Systems
Atlanta November 9th
Scala Spark
Data Modelling for distributed systems
Axon

With ZIO's approach to dependencies, you get many desirable characteristics at compile-time, using standard language features.
Your services are defined as classes with constructor arguments, just as in any vanilla Scala application.
No annotations that kick off impenetrable wiring logic outside your normal code.

For any given service in your application, you define what it needs in order to execute.
Finally, when it is time to build your application, all of these pieces can be provided in one, flat space.
Each component will automatically find its dependencies, and make itself available to other components that need it.

To aid further in understanding your application architecture, you can visualize the dependency graph with a single line.

You can also do things that simply are not possible in other approaches, such as sharing a single instance of a dependency across multiple test classes, or even multiple applications.

## James

One reason to modularize an application into "parts" is so that when those parts need other "parts", the relationship between the parts can be expressed in some way and then also changed depending on the needs for a given execution path.  
Typically, this approach to breaking things into parts and expressing the other parts needed by each part, is called "Dependency Injection."

... Why is it called "Dependency Injection" ?
Avoid having to explicitly pass things down the call chain.

There is one way to express dependencies.  

Let's consider an example: Given a function that fetches Accounts from a database, the necessary parts might be a `DatabaseService` which provides database connections and a `UserService` which provides the access controls.  By separating these dependencies our from the functionality of fetching accounts, tests can utilize some method of "faking" or "mocking" the dependencies to simulate the actual dependency.

In the world of Java these dependent parts are usually expressed through annotations (e.g. `@Autowired` in Spring).  But these approaches are "impure" (require mutability), often rely on runtime magic (e.g. reflection), and require everything that uses the annotations to be created through a Dependency Injection manager, complicating construction flow.  An alternative to this approach is to use "Constructor Injection" which avoids some of the pitfalls associated with "Field Injection" but doesn't resolve some of the underlying issues, including the ability for dependencies to be expressed at compile time.

If instead functionality expressed its dependencies through the type system, the compiler could verify that the needed parts are in-fact available given a particular path of execution (e.g. main app, test suite one, test suite two).  Given the Account service example, it might look something like:

```scala
def getUserAccounts(user: User) = {
  defer {
    val databaseService = ZIO.service[DatabaseService].run
    val userService = ZIO.service[UserService].run
    userService.withConnection { connection =>
      connection.queryUserAccounts(user)
    }
  }
}
```

The non-magic, yet magical part of this is that to use `getUserAccounts` you have to provide the dependencies somewhere in the call chain.  
Let's say you try calling `getUserAccounts` directly in an application:
```scala
runDemo {
  getUserAccounts(User(1))
}
```

This results in a compile error because the required dependencies for calling `getUserAccounts` have not been satisfied anywhere in the call chain.  
Similarly, if you try to call this from a test, it will also result in a compile error. 
Dependencies must be fulfilled somewhere.  
So you may provide the dependencies using "live" dependencies which do the real / production behavior, like:
```scala
runDemo {
  getUserAccounts(User(1))
    .provide(LiveDatabaseService && LiveUserService)
}
```

But in a test you might provide "mock" implementations:
```scala
runSpec {
  getUserAccounts(User(1))
    .provide(TestDatabaseService && TestUserService)
}
```

Dependencies on effects propagate to effects which use effects.  For example, if you have an HTTP handler which calls the `getUserAccounts` effect, the necessary dependencies, if not provided directly to the effect, propagate to the handler effect, and then the HTTP app, etc.  For example:
```scala
HttpHandler.get("/accounts") {
  getUserAccounts(User(1))
}.provide(LiveDatabaseService && LiveUserService)
```

This approach enables flexibility with how and where the dependencies are satisfied.  So you could also do something like:
```scala
HttpHandler.get("/accounts") {
  getUserAccounts(User(1)).provide(LiveDatabaseService)
}.provide(LiveUserService)
```

Values to convey:
 - Layer Graph
    - (Effect A needs Layer X, Effect B calls Effect A, Effect C calls Effect B)
    - Now Effect A, B, C need Layer X unless the layer is provided somewhere in between
    - Cycles are a compile error
    - Attempting to provide the same layer type multiple times is a compile error
    - Visualization with Mermaid
 - Layer Resourcefulness
   - Layers can have setup & teardown (open & close)


# DI-Wow!
- TODO Decide where/how to demo test implementations
```scala mdoc:silent
// Explain private constructor approach
case class Dough private ()

object Dough:
  val letRise: ZIO[Dough, Nothing, Unit] =
    ZIO.debug("Dough is rising")

  val fresh: ZLayer[Any, Nothing, Dough] =
    ZLayer.derive[Dough].tap( _ => ZIO.succeed(println("Making Fresh Dough")))
```

### Step 1: Effects can express dependencies

Effects can't be run until their dependencies have been fulfilled

TODO: Decide what to do about the compiler error differences between these approaches

```scala mdoc:fail
object LetDoughRiseNoDough extends ZIOAppDefault:
  override def run = Dough.letRise
```

```scala mdoc:fail
// TODO Consider weirdness of provide with no args
runDemo:
  Dough.letRise.provide()
```

### Step 2: Provide Dependencies to Effects

Then the effect can be run.

```scala mdoc
runDemo:
  Dough
    .letRise
    .provide:
      Dough.fresh
```

For code organization, and legibility at call sites, we are defining several layers within the `Heat` companion object.
They will all be used soon.

```scala mdoc
case class Heat private ()
object Heat:
  val oven: ZLayer[Any, Nothing, Heat] =
    ZLayer.derive[Heat].tap( _ => ZIO.succeed(println("Heating Oven")))

  val toaster: ZLayer[Any, Nothing, Heat] =
    ZLayer.derive[Heat]

  val broken: ZLayer[Any, String, Nothing] =
    ZLayer.fail("**Power Out**")
```


### Step 3: Effects can require multiple dependencies
```scala mdoc
// Restore private constructor after failure scenario is dialed in
case class Bread()

object Bread:
  val make: ZIO[Heat & Dough, Nothing, Bread] =
    ZIO.succeed(Bread())

  // TODO Explain ZLayer.fromZIO in prose
  // immediately before/after this
  val homemade
      : ZLayer[Heat & Dough, Nothing, Bread] =
    ZLayer.fromZIO:
      make
    .tap( _ => ZIO.succeed(println("Making Homemade Bread")))

  val storeBought: ZLayer[Any, Nothing, Bread] =
    ZLayer.derive[Bread]
      .tap( _ => ZIO.succeed(println("Buying Bread")))

  val eat: ZIO[Bread, Nothing, String] =
    ZIO.succeed("Eating bread!")

    /* defer:
     * println("Eating bread!")
     * ZIO.succeed(()).run */
```


```scala mdoc
runDemo:
  Bread.make.provide(Dough.fresh, Heat.oven)
```

// todo: explore a Scala 3 provideSome that doesn't need to specify the remaining types
//
//val boringSandwich: ZIO[Jelly, Nothing, BreadWithJelly] =
//  makeBreadWithJelly.provideSome [Jelly] (storeBread)

### Step 4: Dependencies can "automatically" assemble to fulfill the needs of an effect

Something around how like typical DI, the "graph" of dependencies gets resolved "for you"
This typically happens in some completely new/custom phase, that does follow standard code paths.

```scala mdoc
// TODO Figure out why Bread.eat debug isn't showing up
runDemo:
  Bread
    .eat
    .provide(
      // Highlight that homemade needs the other
      // dependencies.
      Bread.homemade,
      Dough.fresh,
      Heat.oven
    )
```


### Step 5: Different effects can require the same dependency
Eventually, we grow tired of eating plain `Bread` and decide to start making `Toast`.
Both of these processes require `Heat`.
```scala mdoc
// Is it worth the complexity of making this private?
// It would keep people from creating Toasts without using the make method
case class Toast private ()

object Toast:
  val make: ZIO[Heat & Bread, Nothing, Toast] =
    ZIO.succeed:
      println("Making toast")
      Toast()
```

It is possible to also use the oven to provide `Heat` to make the `Toast`.

The dependencies are based on the type, so in this case both
Toast.make and Bread.make require heat, but 


Notice - Even though we provide the same dependencies in this example, Heat.oven is _also_ required by Toast.make
```scala mdoc
runDemo:
  Toast
    .make
    .provide(
      // effects can require & provide
      // dependencies
      Bread.homemade,
      Dough.fresh,
      Heat.oven
    )
```

However, the oven uses a lot of energy to make `Toast`.
It would be great if we can instead use our dedicated toaster!

### Step 6: Dependencies are based on types and must be uniquely provided

```scala mdoc:fail
runDemo:
  Toast
    .make
    .provide(
      Dough.fresh,
      Bread.homemade,
      Heat.oven,
      Heat.toaster
    )
```
Unfortunately our program is now ambiguous.
It cannot decide if we should be making `Toast` in the oven, `Bread` in the toaster, or any other combination.

### Step 7: Providing Dependencies at Different Levels
This enables other effects that use them to provide their own dependencies of the same type

```scala mdoc
runDemo:
  val bread =
    ZLayer.fromZIO:
      Bread.make.provide(Dough.fresh, Heat.oven)

  Toast.make.provide(bread, Heat.toaster)
```

### Step 8: Dependencies can fail

```scala mdoc:invisible
import zio.Runtime.default.unsafe
object Bread2:
  val invocations =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe = u
      unsafe
        .run(Ref.make(0))
        .getOrThrowFiberFailure()
    )

  def reset() =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe = u
      unsafe
        .run(invocations.set(0))
        .getOrThrowFiberFailure()
    )

  val forcedFailure: ZIO[Any, String, Bread] =
    defer:
      println("**Power out**")
      ZIO
        .when(true)(
          ZIO.fail("**Power out Rez**")
        )
        .map(_ => ???)
        .run
      ZIO.succeed(Bread()).run

  def attempt(
      invocations: Ref[Int]
  ): ZIO[Any, String, Bread] =
    invocations
      .updateAndGet(_ + 1)
      .flatMap {
        case cnt if cnt < 3 =>
          forcedFailure
//              ???
        // Avoid manual Bread construction here
        case _ =>
          defer:
            println("Power is on")
            ZIO.succeed(Bread()).run
      }

  // Already constructed elsewhere, that we don't
  // control
  val fromFriend: ZLayer[Any, String, Bread] =
    ZLayer.fromZIO:
      Bread2.attempt(invocations)
end Bread2
```

TODO Explain `.build` before using it to demo layer construction

```scala mdoc
Bread2.fromFriend
```

```scala mdoc
runDemo:
  Bread.eat.provide(Bread2.fromFriend)
```

```scala mdoc:invisible
Bread2.reset()
```

### Step 9: Dependency Retries

```scala mdoc
runDemo:
  val bread =
    Bread2.fromFriend.retry(Schedule.recurs(3))

  Bread.eat.provide(bread)
```

### Step 10: Dependency Fallback

```scala mdoc:invisible
Bread2.reset()
```

```scala mdoc
runDemo:
  val bread =
    Bread2
      .fromFriend
      .orElse:
        Bread.storeBought

  Toast.make.provide(bread, Heat.toaster)
```

### Step 11: Layer Retry + Fallback?

Maybe retry on the ZLayer eg. (BreadDough.rancid, Heat.brokenFor10Seconds)

```scala mdoc:invisible
Bread2.reset()
```

```scala mdoc
runDemo:
  Bread2
    .fromFriend
    .retry(Schedule.recurs(1))
    .orElse:
      Bread.storeBought
    .build // TODO Stop using build, if possible
    .debug
```
