# Dependency Injection

## Bill

## How to Wow
- Successful, moderately complex application startup
- Compilation errors whenever something is missing or conflicting in the dependency graph
- Demonstrate runtime errors during application startup
- Swap out implementations for testing

With ZIO's approach to dependencies, you get all the desirable characteristics at compile-time, using standard language features.
Your services are defined as classes with constructor arguments, just as in any vanilla Scala application.
No annotations that kick off impenetrable wiring logic outside your normal code.
For any given service in your application, you define what it needs in order to execute.
Finally, when it is time to build your application, all of these pieces can be provided in one, flat space.
Each component will automatically find its dependencies, and make itself available to other components that need it.

To aid further in understanding your application architecture, you can visualize the dependency graph with a single line.

You can also do things that simply are not possible in other approaches, such as sharing a single instance of a dependency across multiple test classes, or even multiple applications.

## James

One reason to modularize an application into "parts" is so that when those parts need other "parts", the relationship between the parts can be expressed in some way and then also changed depending on the needs for a given execution path.  Typically, this approach to breaking things into parts and expressing the other parts needed by each part, is called "Dependency Injection."

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

The non-magic, yet magical part of this is that to use `getUserAccounts` you have to provide the dependencies somewhere in the call chain.  Let's say you try calling `getUserAccounts` directly in an application:
```scala
runDemo {
  getUserAccounts(User(1))
}
```

This results in a compile error because the required dependencies for calling `getUserAccounts` have not been satisfied anywhere in the call chain.  Similarly, if you try to call this from a test, it will also result in a compile error.  Dependencies must be fulfilled somewhere.  So you may provide the dependencies using "live" dependencies which do the real / production behavior, like:
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
```scala mdoc
case class Dough()

val letDoughRise: ZIO[Dough, Nothing, Unit] =
  ZIO.debug("Dough is rising")
```

### Step 1: Effects can express dependencies

Effects can't be run until their dependencies have been fulfilled

TODO: Decide what to do about the compiler error differences between these approaches

```scala mdoc:fail
object LetDoughRiseNoDough extends ZIOAppDefault:
  override def run = letDoughRise
```

```scala mdoc:fail
runDemo:
  letDoughRise
```

```scala mdoc
object Dough:
  val fresh: ZLayer[Any, Nothing, Dough] =
    ZLayer.derive[Dough]
```

### Step 2: Provide Dependencies to effects

Then the effect can be run.

```scala mdoc
runDemo:
  letDoughRise.provide:
    Dough.fresh
```

Note: not all the Heat vals are used right away
Do we organize differently or just introduce the kinds of heats?

```scala mdoc
case class Heat()
object Heat:
  val oven: ZLayer[Any, Nothing, Heat] =
    ZLayer.derive[Heat]

  val toaster: ZLayer[Any, Nothing, Heat] =
    ZLayer.derive[Heat]

  val broken: ZLayer[Any, String, Nothing] =
    ZLayer.fail("power out")
```

```scala mdoc
case class Bread()
```

### Step 3: Effects can require multiple dependencies
```scala mdoc
object Bread:
  val make: ZIO[Heat & Dough, Nothing, Bread] =
    ZIO.succeed(Bread())

  val storeBought: ZLayer[Any, Nothing, Bread] =
    ZLayer.derive[Bread]

  val homemade
      : ZLayer[Heat & Dough, Nothing, Bread] =
    ZLayer.fromZIO:
      make

  val eatBread: ZIO[Bread, Nothing, Unit] =
    ZIO.unit
```


```scala mdoc
runDemo:
  Bread.make.provide(Dough.fresh, Heat.oven)
```

// todo: explore a Scala 3 provideSome that doesn't need to specify the remaining types
//
//val boringSandwich: ZIO[Jelly, Nothing, BreadWithJelly] =
//  makeBreadWithJelly.provideSome [Jelly] (storeBread)

// note: note all of the Bread vals are used right away
//       Do we organize differently or just introduce the kinds of bread & bread actions

### Step 4: Dependencies can "automatically" assemble to fulfill the needs of an effect

Something around how like typical DI, the "graph" of dependencies gets resolved "for you"
This typically happens in some completely new/custom phase, that does follow standard code paths.

```scala mdoc
runDemo:
  Bread
    .eatBread
    .provide(
      Bread.homemade,
      Dough.fresh,
      Heat.oven
    )
```

Dependencies of effects can have their own dependencies

```scala mdoc
// Is it worth the complexity of making this private?
// It would keep people from creating Toasts without using the make method
case class Toast private ()

object Toast:
  val make: ZIO[Heat & Bread, Nothing, Toast] =
    ZIO.succeed(Toast())
```

### Step 5: Different effects can require the same dependency

The dependencies are based on the type, so in this case both
Toast.make and Bread.make require heat, but we likely do not want to
use the oven for both making bread and toast

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

### Step 6: Dependencies are based on types and must be uniquely provided

Heat can't be provided twice.

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

### Step 7: Effects can have their dependencies provided
This enables other effects that use them to provide their own dependencies of the same type

```scala mdoc
runDemo:
  val bread =
    ZLayer.fromZIO(
      Bread.make.provide(Dough.fresh, Heat.oven)
    )

  Toast.make.provide(bread, Heat.toaster)
```

### Step 8: Dependencies can fail

```scala mdoc
runDemo:
  Bread
    .make
    .provide(Dough.fresh, Heat.broken)
    .flip // Get error as result
    // todo: prettyPrintError extension?
    .debug
```

### Step 9: On dependency failure, we can fallback

```scala mdoc
runDemo:
  val attemptBreadWithBrokenOven =
    Bread.make.provide(Dough.fresh, Heat.broken)

  val bread =
    ZLayer
      .fromZIO:
        attemptBreadWithBrokenOven
      .orElse:
        Bread.storeBought

  Toast.make.provide(bread, Heat.toaster)
```

### Step 10: Layer Retries?

Maybe retry on the ZLayer (BreadDough.rancid, Heat.brokenFor10Seconds)