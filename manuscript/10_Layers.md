# Layers

## Creating

## Composing

Managing and wiring dependencies has been a perennial challenge in software development.

ZIO provides the `ZLayer` class to solve many of the problems in this space.
If you pay the modest, consistent cost of constructing pieces of your application as `ZLayer`s, you will get benefits that scale with the complexity of your project.
Consistent with `ZIO` itself, `ZLayer` has 3 type parameters that represent:

- What it needs from the environment
- How it can fail
- What it produces when successful.

With the same type parameters, and many of the same methods, you might be wondering why we even need a separate data type - why not just use `ZIO` itself for our dependencies?
The environment type parameter for `ZLayer` maps directly to unique, singleton services in your application.
The environment type parameter for `ZIO` might have *many* possible instances.
`ZLayer` provides additional behaviors that are valuable specifically in this domain.
Typically, you only want a single instance of a dependency to exist across your application.
This may be to reduce memory/resource usage, or even to ensure basic correctness.
`ZLayer` output values are shared maximally by default.
They also build in scope management that will ensure resource cleanup in asynchronous, fallible situations.


==============

Imagine a `ServiceX` that is needed by 20 diverse functions across your stack.
Usually `ServiceX` has exactly one instance/implementation should be used throughout your application.

```scala
case class ServiceX():
  val retrieveImportantData
      : ZIO[Any, Nothing, String] = ???
```
{{ TODO: Should we show a class-based approach, or just go straight to functions? }}
```scala
case class UserManagement(serviceX: ServiceX)

case class StatisticsCalculator(
    serviceX: ServiceX
)

case class SecurityModule(serviceX: ServiceX)

case class LandingPage(
    statisticsCalculator: StatisticsCalculator
)
```

## Historic Approaches

### Manual Wiring

```scala
case class Application(
    userManagment: UserManagement,
    securityModule: SecurityModule,
    landingPage: LandingPage
)

def construct(): Application =
  val serviceX = ServiceX()
  Application(
    UserManagement(serviceX),
    SecurityModule(serviceX),
    LandingPage(StatisticsCalculator(serviceX))
  )
```

Even in this tiny example, the downsides are already starting to show.

- We have to copy/paste `serviceX` numerous times
- We have to manage multiple levels of dependencies. `LandingPage` and `ServiceImplentation` have to be manually connected.


### Annotations

Pros
- "Easy" in the sense that they do not require much code at the use-site
- Smoother refactoring, as the injection system will determine what needs to be passed around

Cons
- Does not follow normal control flow or composition
- Typically, relies on some framework-level processing that is not easily controlled by the user

## Traits

Before looking at the official ZIO implementation, we will create a simpler version.

TODO: Decide whether explaining this pattern belongs in a standalone section.
It is important in isolation, but probably hard to appreciate without a use-case, and `Console` is likely the simplest example.

The pattern used here is fundamental to designing composable, ergonomic ZIO `Services`.

1. Create a `trait` with the needed functions.
2. Create an implementation of the `trait`.
3. (Optional) Provide implementation instance in a `Layer` as a `object` field - `live`.

We will go through each of these steps in detail in this chapter, and more concisely in the rest.
Steps 1 and 2 steps will be familiar to many programmers.
Steps 3 is less familiar, and might be harder to appreciate.
We endeavor in the following chapters to make a compelling case for them.
If we succeed, the reader will use them when creating their own Effects.

### One: Create the trait

This `trait` represents effectful code that we need to interact with.

```scala
trait Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit]
```

### Two: Create the implementation

```scala
object ConsoleLive extends Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit] =
    // TODO Get this working without Predef
    ZIO.succeed(Predef.println(output))
```

```scala
case class Logic(console: Console):
  val invoke: ZIO[Any, Nothing, Unit] =
    defer {
      console.printLine("Hello").run
      console.printLine("World").run
    }
```

However, providing dependencies to the logic is still tedious.

```scala
runDemo(Logic(ConsoleLive).invoke)
// Hello
// World
// ()
```

### Three: Create `object Effect.live` field

Rather than making each caller wrap our instance in a `Layer`, we can do that a single time in our companion.

```scala
object Console:
  val live: ZLayer[Any, Nothing, Console] =
    ZLayer.succeed[Console](ConsoleLive)
```
More important than removing repetition - using 1 unique Layer instance per type allows us to share it across our application.

Now executing our code is as simple as describing it.

```scala
runDemo(
  ZIO
    .serviceWithZIO[Logic](_.invoke)
    .provide(
      Console.live,
      ZLayer.fromFunction(Logic.apply _)
    )
)
// Hello
// World
// ()
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/10_Layers.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/layers/Festival.scala
```scala
package layers

import zio.ZIO.debug

case class Toilets()
val toilets = activityLayer(entity = Toilets())

case class Stage()
val stage: ZLayer[Any, Nothing, Stage] =
  activityLayer(
    entity = Stage(),
    setupSteps = ("Transporting", 2.seconds),
    ("Building", 4.seconds)
  )

case class Permit()
val permit: ZLayer[Any, Nothing, Permit] =
  activityLayer(
    entity = Permit(),
    setupSteps = ("Legal Request", 5.seconds)
  )

def activityLayer[T: Tag](
    entity: T,
    setupSteps: (String, Duration)*
) =
  ZLayer.scoped(
    ZIO.acquireRelease(
      defer:
        ZIO
          .debug:
            entity.toString + " ACQUIRE"
          .run
        ZIO
          .foreach(setupSteps):
            case (name, duration) =>
              activity(
                entity.toString,
                name,
                duration
              )
          .run
        entity
    )(_ => debug(entity.toString + " RELEASE"))
  )

def activity(
    entity: String,
    name: String,
    duration: Duration
) =
  defer:
    debug:
      s"$entity: BEGIN $name"
    .run
    debug:
      s"$entity: END $name"
    .delay(duration)
      .run

case class Venue(stage: Stage, permit: Permit)
val venue = ZLayer.fromFunction(Venue.apply)

case class SoundSystem()
val soundSystem
    : ZLayer[Any, Nothing, SoundSystem] =
  ZLayer.succeed(SoundSystem())

case class Festival(
    toilets: Toilets,
    venue: Venue,
    soundSystem: SoundSystem,
    security: Security
)

val festival =
  ZLayer.scoped {
    ZIO.acquireRelease {
      defer:
        debug("FESTIVAL: We are all set!").run
        Festival(
          ZIO.service[Toilets].run,
          ZIO.service[Venue].run,
          ZIO.service[SoundSystem].run,
          ZIO.service[Security].run
        )
    } { _ =>
      debug(
        "FESTIVAL: Good job, everyone. Close it down!"
      )
    }
  }

case class Security(toilets: Toilets)

val security
    : ZLayer[Toilets, Nothing, Security] =
  ZLayer.scoped {
    ZIO.acquireRelease {
      defer:
        debug("SECURITY: Ready").run
        Security(ZIO.service[Toilets].run)
    } { _ =>
      debug("SECURITY: Going home")
    }
  }

```

