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
  *   - Heat/Infrared
  *   - Sound Should alert by:
  *   - Quiet, local beep
  *   - Loud Local Siren
  *   - Ping security company
  *   - Notify police
  */
object SecuritySystem:
  // TODO Why can't I use this???

  val accessMotionDetector: ZIO[
    scenarios.MotionDetector,
    scenarios.HardwareFailure,
    scenarios.Pixels
  ] = ZIO.serviceWithZIO(_.amountOfMotion())

  def securityLoop(
      amountOfMotion: Pixels,
      acousticDetector: ZIO[
        Any,
        scala.concurrent.TimeoutException |
          scenarios.HardwareFailure,
        scenarios.Decibels
      ]
  ): ZIO[
    SirenX,
    scala.concurrent.TimeoutException |
      HardwareFailure,
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
        determineResponse(
          amountOfMotion,
          noise
        )
      securityResponse match
        case Relax =>
          ZIO.debug("No need to panic").run
        case LowBeep =>
          SirenX.lowBeep.run
        case LoudSiren =>
          SirenX.loudSiren.run
    }

  @annotation.nowarn
  def shouldAlertServices[
      T
        <: MotionDetector &
          SirenX & AcousticDetectorX
  ](): ZIO[
    T,
    scenarios.HardwareFailure | TimeoutException,
    String
  ] =
    defer {
      val amountOfMotion =
        MotionDetector
          .acquireMotionMeasurementSource()
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

  def shouldTrigger(
      amountOfMotion: Pixels,
  ): Boolean =
    amountOfMotion.value > 10

  def determineResponse(
      amountOfMotion: Pixels,
      noise: Decibels
  ): SecurityResponse =
    val numberOfAlerts =
      List(
        amountOfMotion.value > 50,
        noise.value > 15
      ).count(_ == true)

    if (numberOfAlerts == 0)
      Relax
    else if (numberOfAlerts == 1)
      LowBeep
    else
      LoudSiren
  end determineResponse

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
object LowBeep   extends SecurityResponse
object LoudSiren extends SecurityResponse

trait HardwareFailure

case class Decibels(value: Int)
case class Pixels(value: Int)

trait MotionDetector:
  def amountOfMotion()
      : ZIO[Any, HardwareFailure, Pixels]

object MotionDetector:

  object LiveMotionDetector
      extends MotionDetector:
    override def amountOfMotion()
        : ZIO[Any, HardwareFailure, Pixels] =
      ZIO.succeed(Pixels(30))

  def acquireMotionMeasurementSource(): ZIO[
    MotionDetector,
    HardwareFailure,
    Pixels
  ] =
    ZIO
      .service[MotionDetector]
      .flatMap(_.amountOfMotion())

  val live
      : ZLayer[Any, Nothing, MotionDetector] =
    ZLayer.succeed(LiveMotionDetector)
end MotionDetector

trait AcousticDetectorX:
  def acquireDetector(): ZIO[Any, Nothing, ZIO[
    Any,
    TimeoutException | scenarios.HardwareFailure,
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
              TimeoutException |
                scenarios.HardwareFailure,
              Decibels
            ]] = scheduledValues(value, values*)
    )

  // This is preeeetty gnarly. How can we
  // improve?
  def acquireDetector[
      T <: scenarios.AcousticDetectorX: Tag
  ]: ZIO[T, Nothing, ZIO[
    Any,
    scala.concurrent.TimeoutException |
      scenarios.HardwareFailure,
    scenarios.Decibels
  ]] =
    ZIO.serviceWithZIO[
      scenarios.AcousticDetectorX
    ](_.acquireDetector())

end AcousticDetectorX

object Siren:
  trait ServiceX:
    def lowBeep(): ZIO[
      Any,
      scenarios.HardwareFailure,
      Unit
    ]

  val live
      : ZLayer[Any, Nothing, Siren.ServiceX] =
    ZLayer.succeed(
      // that same service we wrote above
      new ServiceX:

        def lowBeep(): ZIO[
          Any,
          scenarios.HardwareFailure,
          Unit
        ] = ZIO.debug("beeeeeeeeeep")
    )
end Siren

trait SirenX:
  def lowBeep()
      : ZIO[Any, scenarios.HardwareFailure, Unit]

  def loudSiren()
      : ZIO[Any, scenarios.HardwareFailure, Unit]

object SirenX:
  object SirenXLive extends SirenX:
    def lowBeep(): ZIO[
      Any,
      scenarios.HardwareFailure,
      Unit
    ] = ZIO.debug("beeeeeeeeeep")

    def loudSiren(): ZIO[
      Any,
      scenarios.HardwareFailure,
      Unit
    ] = ZIO.debug("WOOOO EEEE WOOOOO EEEE")

  val live: ZLayer[Any, Nothing, SirenX] =
    ZLayer.succeed(SirenXLive)

  val lowBeep: ZIO[
    SirenX,
    scenarios.HardwareFailure,
    Unit
  ] = ZIO.serviceWithZIO(_.lowBeep())

  val loudSiren: ZIO[
    SirenX,
    scenarios.HardwareFailure,
    Unit
  ] = ZIO.serviceWithZIO(_.loudSiren())

end SirenX

@annotation.nowarn
class SensorD[T](
    z: ZIO[
      Any,
      HardwareFailure,
      ZIO[Any, TimeoutException, T]
    ]
)

// TODO Figure out how to use this
object SensorData:
  def live[T, Y: zio.Tag](
      c: ZIO[
        Any,
        HardwareFailure,
        ZIO[Any, TimeoutException, T]
      ] => Y,
      value: (Duration, T),
      values: (Duration, T)*
  ): ZLayer[Any, Nothing, Y] =
    ZLayer.succeed(
      // that same service we wrote above
      c(scheduledValues[T](value, values*))
    )

  def liveS[T: zio.Tag](
      value: (Duration, T),
      values: (Duration, T)*
  ): ZLayer[Any, Nothing, SensorD[T]] =
    ZLayer.succeed(
      // that same service we wrote above
      SensorD(scheduledValues[T](value, values*))
    )
end SensorData
