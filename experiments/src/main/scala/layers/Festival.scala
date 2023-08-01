package layers

import zio.ZIO.debug

case class Toilets()
val toilets = activityLayer(entity = Toilets())

case class Stage()
val stage =
  activityLayer(
    entity = Stage(),
    setupSteps = ("Transporting", 2.seconds),
    ("Building", 4.seconds)
  )

case class Permit()
val permit =
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
val speakers =
  ZLayer.scoped(
    ZIO.acquireRelease(
      debug("SPEAKERS: Positioning") *>
        ZIO.succeed(Speakers())
    )(_ => debug("SPEAKERS: Packing up"))
  )
case class Amplifiers()
val amplifiers =
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
case class Fencing()
val fencing: ZLayer[Any, Nothing, Fencing] =
  ZLayer.scoped(
    ZIO.acquireRelease(
      debug("FENCING: Surrounding the area") *>
        ZIO.succeed(Fencing())
    )(_ => debug("FENCING: Tearing down"))
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
    fencing: Fencing,
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
          ZIO.service[Fencing].run,
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
