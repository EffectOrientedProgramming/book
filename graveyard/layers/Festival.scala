package layers

import zio.ZIO.debug

case class Toilets()
val toilets =
  activityLayer(entity =
    Toilets()
  )

case class Stage()
val stage: ZLayer[Any, Nothing, Stage] =
  activityLayer(
    entity =
      Stage(),
    setupSteps =
      ("Transporting", 2.seconds),
    ("Building", 4.seconds)
  )

case class Permit()
val permit: ZLayer[Any, Nothing, Permit] =
  activityLayer(
    entity =
      Permit(),
    setupSteps =
      ("Legal Request", 5.seconds)
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
    )(
      _ => debug(entity.toString + " RELEASE")
    )
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
val venue =
  ZLayer.fromFunction(Venue.apply)

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
    } {
      _ =>
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
    } {
      _ =>
        debug("SECURITY: Going home")
    }
  }
