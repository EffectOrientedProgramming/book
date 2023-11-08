package scenarios

import izumi.reflect.Tag
import time.scheduledValues

import scala.concurrent.TimeoutException

case class SecuritySystemX(
    motionDetector: MotionDetector,
    acousticDetectorX: AcousticDetectorX
)

object SecuritySystemX:
  val live =
    ZLayer.fromFunction(SecuritySystemX.apply _)

/** Situations: Security System: Should monitor
  *   - Motion
  *   - Sound Should alert by:
  *   - Quiet, local beep
  *   - Loud Local Siren
  *   - Ping security company
  *   - Notify police
  */
object SecuritySystem:

  def securityLoop(
      amountOfMotion: Pixels,
      acousticDetector: ZIO[
        Any,
        scala.concurrent.TimeoutException,
        scenarios.Decibels
      ]
  ): ZIO[
    SirenX,
    scala.concurrent.TimeoutException,
    Unit
  ] =
    defer {
      val noise = acousticDetector.run
      ZIO
        .debug(
          s"Motion: $amountOfMotion  Noise: $noise"
        )
        .run
      val securityResponse =
        determineResponse(amountOfMotion, noise)
      securityResponse match
        case Relax =>
          ZIO.debug("No need to panic").run
        case LoudSiren =>
          SirenX.loudSiren.run
    }

  @annotation.nowarn
  def shouldAlertServices[
      T
        <: MotionDetector & SirenX &
          AcousticDetectorX
  ](): ZIO[
    T,
    TimeoutException,
    String
  ] =
    defer {
      val amountOfMotion =
        MotionDetector
          .amountOfMotion
          .run

      val acousticDetector =
        AcousticDetectorX.acquireDetector.run

      securityLoop(
        amountOfMotion,
        acousticDetector
      ).repeat(
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

end SecuritySystem

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
  def acquireDetector(): ZIO[Any, Nothing, ZIO[
    Any,
    TimeoutException,
    Decibels
  ]]

object AcousticDetectorX:

  def apply(
      value: (Duration, Decibels),
      values: (Duration, Decibels)*
  ): ZLayer[Any, Nothing, AcousticDetectorX] =
    ZLayer.succeed(
      // that same service we wrote above
      new AcousticDetectorX:
        override def acquireDetector()
            : ZIO[Any, Nothing, ZIO[
              Any,
              TimeoutException,
              Decibels
            ]] = scheduledValues(value, values*)
    )

  // This is preeeetty gnarly. How can we
  // improve?
  def acquireDetector[
      T <: scenarios.AcousticDetectorX: Tag
  ]: ZIO[T, Nothing, ZIO[
    Any,
    scala.concurrent.TimeoutException,
    scenarios.Decibels
  ]] =
    ZIO.serviceWithZIO[
      scenarios.AcousticDetectorX
    ](_.acquireDetector())

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
    SirenX,
    Nothing,
    Unit
  ] = ZIO.serviceWithZIO(_.loudSiren())

end SirenX