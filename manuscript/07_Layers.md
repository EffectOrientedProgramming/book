# Managing Dependencies

Managing and wiring dependencies has been a perennial challenge in software development.

ZIO provides the `ZLayer` class to solve many of the problems in this space.
If you pay the modest, consistent cost of constructing pieces of your application as `ZLayer`s, you will get benefits that scale with the complexity of your project.
Consistent with `ZIO` itself, `ZLayer` has 3 type parameters that represent:

- What it needs from the environment
- How it can fail
- What it produces when successful.

With the same type parameters, and many of the same methods, you might be wondering why we even need a separate data type - why not just use `ZIO` itself for our dependencies?
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


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/07_Layers.md)


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
      ZIO.debug(entity.toString + " ACQUIRE") *>
        ZIO.foreach(setupSteps) {
          case (name, duration) =>
            activity(
              entity.toString,
              name,
              duration
            )
        } *> ZIO.succeed(entity)
    )(_ => debug(entity.toString + " RELEASE"))
  )

def activity(
    entity: String,
    name: String,
    duration: Duration
) =
  debug(s"$entity: BEGIN $name") *>
    debug(s"$entity: END $name").delay(duration)

case class Venue(stage: Stage, permit: Permit)
val venue = ZLayer.fromFunction(Venue.apply)

case class Speakers()
val speakers: ZLayer[Any, Nothing, Speakers] =
  ZLayer.scoped(
    ZIO.acquireRelease(
      debug("SPEAKERS: Positioning") *>
        ZIO.succeed(Speakers())
    )(_ => debug("SPEAKERS: Packing up"))
  )
case class Amplifiers()
val amplifiers
    : ZLayer[Any, Nothing, Amplifiers] =
  ZLayer.scoped(
    ZIO.acquireRelease(
      debug("AMPLIFIERS: Positioning") *>
        ZIO.succeed(Amplifiers())
    )(_ => debug("AMPLIFIERS: Putting away"))
  )

case class Wires()
val wires =
  ZLayer.scoped(
    ZIO.acquireRelease(
      debug("WIRES: Unrolling") *>
        ZIO.succeed(Wires())
    )(_ => debug("WIRES: Spooling up"))
  )

case class SoundSystem(
    speakers: Speakers,
    amplifiers: Amplifiers,
    wires: Wires
)
val soundSystem: ZLayer[
  Speakers with Amplifiers with Wires,
  Nothing,
  SoundSystem
] =
  ZLayer.scoped {
    ZIO.acquireRelease {
      defer {
        debug(
          "SOUNDSYSTEM: Hooking up speakers, amplifiers, and wires"
        ).run
        SoundSystem(
          ZIO.service[Speakers].run,
          ZIO.service[Amplifiers].run,
          ZIO.service[Wires].run
        )
      }
    } { _ =>
      debug(
        "SOUNDSYSTEM: Disconnecting speakers, amplifiers, and wires"
      )
    }
  }

val soundSystemShortedOut: ZLayer[
  Speakers with Amplifiers with Wires,
  String,
  SoundSystem
] =
  ZLayer.scoped {
    ZIO.acquireRelease {
      debug(
        "SOUNDSYSTEM: Hooking up speakers, amplifiers, and wires"
      ) *> ZIO.fail("BZZZZ")
    } { _ =>
      debug(
        "SOUNDSYSTEM: Disconnecting speakers, amplifiers, and wires"
      )
    }
  }

case class FoodTruck()
val foodtruck =
  activityLayer(
    entity = FoodTruck(),
    setupSteps = ("Fueling", 2.seconds),
    ("FOODTRUCK: Driving in", 3.seconds)
  )

case class Festival(
    toilets: Toilets,
    venue: Venue,
    soundSystem: SoundSystem,
    foodTruck: FoodTruck,
    security: Security
)

val festival =
  ZLayer.scoped {
    ZIO.acquireRelease {
      defer {
        debug("FESTIVAL: We are all set!").run
        Festival(
          ZIO.service[Toilets].run,
          ZIO.service[Venue].run,
          ZIO.service[SoundSystem].run,
          ZIO.service[FoodTruck].run,
          ZIO.service[Security].run
        )
      }
    } { _ =>
      debug(
        "FESTIVAL: Good job, everyone. Close it down!"
      )
    }
  }

case class Security(
    toilets: Toilets,
    foodTruck: FoodTruck
)

val security: ZLayer[
  Toilets & FoodTruck,
  Nothing,
  Security
] =
  ZLayer.scoped {
    ZIO.acquireRelease {
      defer {
        debug("SECURITY: Ready").run
        Security(
          ZIO.service[Toilets].run,
          ZIO.service[FoodTruck].run
        )
      }
    } { _ =>
      debug("SECURITY: Going home")
    }
  }

```

