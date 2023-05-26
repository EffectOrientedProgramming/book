package layers

import zio.{
  Duration,
  Scope,
  ZIO,
  ZLayer,
  durationInt
}
import zio.ZIO.debug
import zio.direct.*

case class Toilets()
val toilets =
  ZLayer.scoped(
    ZIO.acquireRelease(
      debug("TOILETS: Setting up") *>
        ZIO.succeed(Toilets())
    )(_ => debug("TOILETS: Removing"))
  )
case class Stage()
val stage =
  ZLayer.scoped(
    ZIO.acquireRelease(
      activity(
        "STAGE",
        "Transporting",
        2.seconds
      ) *>
        activity(
          "STAGE",
          "Building",
          4.seconds
        ) *> ZIO.succeed(Stage())
    )(_ => debug("STAGE: Tearing down"))
  )

case class Permit()
val permit =
  ZLayer.scoped(
    ZIO.acquireRelease(
      activity(
        "PERMIT",
        "Legal Request",
        5.seconds
      ) *> ZIO.succeed(Permit())
    )(_ => debug("PERMIT: Relinquished"))
  )

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
  for
    layer <-
      ZLayer.fromFunction(SoundSystem.apply)
    scoped <-
      ZLayer.scoped {
        ZIO.acquireRelease(
          debug(
            "SOUNDSYSTEM: Hooking up speakers, amplifiers, and wires"
          ) *> ZIO.succeed(layer.get)
        )(_ =>
          debug(
            "SOUNDSYSTEM: Disconnecting speakers, amplifiers, and wires"
          )
        )
      }
  yield scoped

val soundSystemShortedOut: ZLayer[
  Speakers with Amplifiers with Wires,
  String,
  SoundSystem
] =
  for
    layer <-
      ZLayer.fromFunction(SoundSystem.apply)
    scoped <-
      ZLayer.scoped {
        ZIO.acquireRelease(
          debug(
            "SOUNDSYSTEM: Hooking up speakers, amplifiers, and wires"
          ) *> ZIO.fail("BZZZZ") *>
            ZIO.succeed(layer.get)
        )(_ =>
          debug(
            "SOUNDSYSTEM: Disconnecting speakers, amplifiers, and wires"
          )
        )
      }
  yield scoped

case class FoodTruck()
val foodtruck =
  ZLayer.scoped(
    ZIO.acquireRelease(
      debug("FOODTRUCK:  Driving in ") *>
        activity(
          "FOODTRUCK",
          "Fueling",
          2.seconds
        ) *> ZIO.succeed(FoodTruck())
    )(_ => debug("FOODTRUCK: Driving out "))
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
  for
    layer <- ZLayer.fromFunction(Festival.apply)
    scoped <-
      ZLayer.scoped {
        ZIO.acquireRelease(
          debug("FESTIVAL: We are all set!") *>
            ZIO.succeed(layer.get)
        )(_ =>
          debug(
            "FESTIVAL: Good job, everyone. Close it down!"
          )
        )
      }
  yield scoped

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

def activity(
    entity: String,
    name: String,
    duration: Duration
) =
  debug(s"$entity: BEGIN $name") *>
    debug(s"$entity: END $name").delay(duration)
