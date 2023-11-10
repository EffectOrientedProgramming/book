package scenarios

import izumi.reflect.Tag
import time.scheduledValues

import scala.concurrent.TimeoutException

case class SecuritySystemX(
    motionDetector: MotionDetector,
    acousticDetectorX: AcousticDetectorX
):

  val securityLoop: ZIO[
    Any,
    scala.concurrent.TimeoutException,
    Unit
  ] =
    defer {
      // TODO Get noiseDetector in a proper way *before* looping
      val noise = acousticDetectorX.monitorNoise.run
      val motion = motionDetector.amountOfMotion.run
      ZIO
        .debug(
          s"Motion: $motion  Noise: $noise"
        )
        .run
      val securityResponse =
        determineResponse(motion, noise)
      securityResponse match
        case Relax =>
          ZIO.debug("No need to panic").run
        case LoudSiren =>
          SirenX.loudSiren.run
    }

  @annotation.nowarn
  def shouldAlertServices(): ZIO[
    Any,
    TimeoutException,
    String
  ] =
    defer {
      securityLoop.repeat(
          Schedule.recurs(5) &&
            Schedule.spaced(1.seconds)
        )
        .run

      "Fin"
    }

  def determineResponse(
                         amountOfMotion: Pixels,
                         noise: Decibels
                       ): SecurityResponse =
    val numberOfAlerts =
      determineBreaches(amountOfMotion, noise)
        .size

    if (numberOfAlerts == 0)
      Relax
    else
      LoudSiren

  def determineBreaches(
                         amountOfMotion: Pixels,
                         noise: Decibels
                       ): Set[SecurityBreach] =
    List(
      Option.when(amountOfMotion.value > 50)(
        SignificantMotion
      ),
      Option.when(noise.value > 15)(LoudNoise)
    ).flatten.toSet

object SecuritySystemX:
  val live =
    ZLayer.fromFunction(SecuritySystemX.apply _)

trait SecurityBreach
object LoudNoise         extends SecurityBreach
object SignificantMotion extends SecurityBreach

trait SecurityResponse
object Relax     extends SecurityResponse
object LoudSiren extends SecurityResponse

case class Decibels(value: Int)
case class Pixels(value: Int)

trait MotionDetector:
  val amountOfMotion
      : ZIO[Any, Nothing, Pixels]

object MotionDetector:

  object LiveMotionDetector
      extends MotionDetector:
    override val amountOfMotion
        : ZIO[Any, Nothing, Pixels] =
      ZIO.succeed(Pixels(30))

  val amountOfMotion: ZIO[
    MotionDetector,
    Nothing,
    Pixels
  ] =
    ZIO
      .service[MotionDetector]
      .flatMap(_.amountOfMotion)

  val live
      : ZLayer[Any, Nothing, MotionDetector] =
    ZLayer.succeed(LiveMotionDetector)
end MotionDetector

trait AcousticDetectorX:
  val monitorNoise: ZIO[
    Any,
    TimeoutException,
    Decibels
  ]

object AcousticDetectorX:
  case class Live(
                   valueProducer: ZIO[Any, TimeoutException, Decibels]
                 ) extends AcousticDetectorX:
    val monitorNoise: ZIO[
      Any,
      TimeoutException,
      Decibels
    ] = valueProducer

  def apply(
      value: (Duration, Decibels),
      values: (Duration, Decibels)*
  ): ZLayer[Any, Nothing, AcousticDetectorX] =
    ZLayer.fromZIO:
      defer:
        val valueProducer: ZIO[Any, TimeoutException, Decibels] =
          scheduledValues(value, values*).run

        // that same service we wrote above
        Live(valueProducer)

end AcousticDetectorX

trait SirenX:
  def loudSiren()
      : ZIO[Any, Nothing, Unit]

object SirenX:
  object SirenXLive extends SirenX:

    def loudSiren(): ZIO[
      Any,
      Nothing,
      Unit
    ] = ZIO.debug("WOOOO EEEE WOOOOO EEEE")

  val live: ZLayer[Any, Nothing, SirenX] =
    ZLayer.succeed(SirenXLive)

  val loudSiren: ZIO[
    Any,
    Nothing,
    Unit
  ] = SirenXLive.loudSiren()

end SirenX