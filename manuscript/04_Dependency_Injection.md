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



## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/04_Dependency_Injection.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/dependency_injection/Recipe.scala
```scala
package dependency_injection

case class BreadDough()

val letDoughRise
    : ZIO[BreadDough, Nothing, Unit] = ZIO.unit

// Step 1: Effects can express dependencies, effects can't be run until their
//         dependencies have been fulfilled
/* object LetDoughRiseNoDough extends
 * ZIOAppDefault:
 * override def run =
 * letDoughRise */

object BreadDough:
  val fresh: ZLayer[Any, Nothing, BreadDough] =
    ZLayer.derive[BreadDough]

// Step 2: Dependencies can be provided on the effect, so that the effect can be run
object LetDoughRise extends ZIOAppDefault:
  override def run =
    letDoughRise.provide:
      BreadDough.fresh

// note: note all of the Heat vals are used right away
//       Do we organize differently or just introduce the kinds of heats
case class Heat()
object Heat:
  val oven: ZLayer[Any, Nothing, Heat] =
    ZLayer.derive[Heat]
  val toaster: ZLayer[Any, Nothing, Heat] =
    ZLayer.derive[Heat]
  val broken: ZLayer[Any, String, Nothing] =
    ZLayer.fail("power out")

// Step 3: Effects can require multiple dependencies
object MakeBread extends ZIOAppDefault:
  override def run =
    Bread
      .makeHomemade
      .provide(BreadDough.fresh, Heat.oven)

// todo: explore a Scala 3 provideSome that doesn't need to specify the remaining types
//
//val boringSandwich: ZIO[Jelly, Nothing, BreadWithJelly] =
//  makeBreadWithJelly.provideSome[Jelly](storeBread)

case class Bread()

// note: note all of the Bread vals are used right away
//       Do we organize differently or just introduce the kinds of bread & bread actions
object Bread:
  val makeHomemade
      : ZIO[Heat & BreadDough, Nothing, Bread] =
    ZIO.succeed(Bread())
  val storeBought: ZLayer[Any, Nothing, Bread] =
    ZLayer.derive[Bread]
  val homemade: ZLayer[
    Heat & BreadDough,
    Nothing,
    Bread
  ] = ZLayer.fromZIO(makeHomemade)

val eatBread: ZIO[Bread, Nothing, Unit] =
  ZIO.unit

// Step 4: Dependencies can "automatically" assemble to fulfill the needs of an effect
//   prose comment: something around how like typical DI, the "graph" of dependencies gets
//     resolved "for you"
object UseBread extends ZIOAppDefault:
  def run =
    eatBread.provide(
      Bread.homemade,
      BreadDough.fresh,
      Heat.oven
    )

// Dependencies of effects can have their own dependencies
case class Toast()

val makeToast
    : ZIO[Heat & Bread, Nothing, Toast] =
  ZIO.succeed(Toast())

// Step 5: Different effects can require the same dependency
//   The dependencies are based on the type, so in this case both
//   makeToast and makeBread require heat, but we likely do not want to
//   use the oven for both making bread and toast
object MakeToast extends ZIOAppDefault:
  override def run =
    makeToast.provide(
      Bread
        .homemade, // effects can provide dependencies but they also propagate their dependencies
      BreadDough.fresh,
      Heat.oven
    )

// Step 6: Dependencies are based on types and must be uniquely provided
// Heat can't be provided twice
// Note: show mdoc compile error
/* object MakeToastConflictingHeat extends
 * ZIOAppDefault:
 * override def run =
 * makeToast.provide( BreadDough.fresh,
 * Bread.homemade, // effects can provide
 * dependencies but they also propagate their
 * dependencies Heat.oven, Heat.toaster, ) */

// Step 7: Effects can have their dependencies provided,
//   enabling other effects that use them to provide their own dependencies of the same type
object MakeToastWithToasterAndOven
    extends ZIOAppDefault:
  override def run =
    val bread =
      ZLayer.fromZIO(
        Bread
          .makeHomemade
          .provide(BreadDough.fresh, Heat.oven)
      )

    makeToast.provide(bread, Heat.toaster)

// Step 8: Dependencies can fail
object MakeBreadWithBrokenOven
    extends ZIOAppDefault:
  override def run =
    Bread
      .makeHomemade
      .provide(BreadDough.fresh, Heat.broken)
      .flip
      .debug // todo: maybe could be a hidden extension method like prettyPrintError

// Step 9: On dependency failure, we can fallback
object MakeBreadWithBrokenOvenFallback
    extends ZIOAppDefault:
  override def run =
    val bread =
      ZLayer
        .fromZIO:
          Bread
            .makeHomemade
            .provide(
              BreadDough.fresh,
              Heat.broken
            )
        .orElse:
          Bread.storeBought

    makeToast.provide(bread, Heat.toaster)

// Step 10: Maybe retry on the ZLayer (BreadDough.rancid, Heat.brokenFor10Seconds)

```


### experiments/src/main/scala/dependency_injection/Simple.scala
```scala
package dependency_injection

class Simple extends ZIOAppDefault:
  val effect1 =
    defer:
      val s = ZIO.service[String].run
      ZIO.debug(s).run

  val effect2 =
    defer:
      val s = ZIO.service[String].run
      ZIO.debug(s.toUpperCase).run

  val effects1and2 =
    defer:
      effect1.run
      effect2.run

  val effect3 =
    val e =
      defer:
        val i = ZIO.service[Int].run
        ZIO.debug(i).run
    e

  val effect1and2and3 =
    defer:
      effects1and2.run
      effect3.run

  override def run =
    effect1and2and3.provide(
      ZLayer
        .fail("asdf")
        .catchAll(_ => ZLayer.succeed("zxcv")),
      ZLayer.succeed(1)
    )
end Simple

```


### experiments/src/main/scala/dependency_injection/Wow.scala
```scala
package dependency_injection

case class A()
case class B()
case class C()
case class D(b: B, c: C)
val d: ZLayer[B & C & Scope, Nothing, D] =
  ZLayer.fromZIO:
    defer:
      val dLocal =
        D(ZIO.service[B].run, ZIO.service[C].run)
      acquireReleaseDebugOnly(dLocal).run

def acquireReleaseDebugOnly[T: Tag](
    instance: T
) =
  val rep =
    instance
      .getClass
      .toString
      .dropWhile(_ != '.')
      .drop(1)
      .replace("$", "")
  ZIO.acquireRelease(
    defer:
      val duration =
        Random.nextIntBounded(500).run
      ZIO.sleep(duration.millis).run
      ZIO.debug(s"Getting $rep").run
      instance
  )(_ =>
    defer:
      val duration =
        Random.nextIntBounded(500).run
      ZIO.sleep(duration.millis).run
      ZIO.debug(s"Releasing $rep").run
  )
end acquireReleaseDebugOnly

def acquireReleaseDebug[T: Tag](instance: T) =
  ZLayer.fromZIO:
    acquireReleaseDebugOnly(instance)

object Wow extends ZIOAppDefault:
  def run =
    defer:
      ZIO.service[A].run
      ZIO.service[D].run
    .provide(
      Scope.default,
      acquireReleaseDebug(A()),
      acquireReleaseDebug(B()),
      acquireReleaseDebug(C()),
      d
    )

```

