package layers

import zio.{ZIO, ZLayer}
import zio.durationInt

case class Toilets()
val toilets =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug("Setting up toilets") *>
        ZIO.succeed(Toilets())
    )(_ => ZIO.debug("Removing toilets"))
  )
case class Stage()
val stage =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug("Building stage") *>
        ZIO.succeed(Stage())
    )(_ => ZIO.debug("Tearing down stage"))
  )

case class Permit()
val permit =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug(
        "PERMIT: Submitted legal request"
      ) *>
        ZIO
          .debug("PERMIT: Granted")
          .delay(5.seconds) *>
        ZIO.succeed(Permit())
    )(_ => ZIO.debug("PERMIT: Relinquished"))
  )

case class Venue(stage: Stage, permit: Permit)
val venue = ZLayer.fromFunction(Venue.apply)

case class Speakers()
val speakers =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug("Positioning speakers") *>
        ZIO.succeed(Speakers())
    )(_ => ZIO.debug("Packing up speakers"))
  )
case class Amplifiers()
val amplifiers =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug("Positioning amplifiers") *>
        ZIO.succeed(Amplifiers())
    )(_ => ZIO.debug("Putting away amplifiers"))
  )
case class Wires()
val wires =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug(
        "Unrolling and laying out wires"
      ) *> ZIO.succeed(Wires())
    )(_ => ZIO.debug("Spooling up wires"))
  )
case class Fencing()
val fencing =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug(
        "Surrounding the area in fenching"
      ) *> ZIO.succeed(Fencing())
    )(_ => ZIO.debug("Tearing down fencing"))
  )
case class SoundSystem(
    speakers: Speakers,
    amplifiers: Amplifiers,
    wires: Wires
)
val soundSystem =
  for
    layer <-
      ZLayer.fromFunction(SoundSystem.apply)
    scoped <-
      ZLayer.scoped {
        ZIO.acquireRelease(
          ZIO.debug(
            "Hooking up speakers, amplifiers, and wires"
          ) *> ZIO.succeed(layer.get)
        )(_ =>
          ZIO.debug(
            "Disconnecting speakers, amplifiers, and wires"
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
          ZIO.debug(
            "Hooking up speakers, amplifiers, and wires"
          ) *> ZIO.fail("BZZZZ") *>
            ZIO.succeed(layer.get)
        )(_ =>
          ZIO.debug(
            "Disconnecting speakers, amplifiers, and wires"
          )
        )
      }
  yield scoped

case class FoodTruck()
val foodtruck =
  ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.debug("Driving in FoodTruck") *>
        ZIO
          .debug("FOODTRUCK: Done fueling")
          .delay(2.seconds) *>
        ZIO.succeed(FoodTruck())
    )(_ => ZIO.debug("Driving out FoodTruck"))
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
          ZIO.debug("We are all set!") *>
            ZIO.succeed(layer.get)
        )(_ =>
          ZIO.debug(
            "Good festival, everyone. Close it down!"
          )
        )
      }
  yield scoped

case class Security(
    toilets: Toilets,
    foodTruck: FoodTruck
)
val security =
  ZLayer.fromFunction(Security.apply)
