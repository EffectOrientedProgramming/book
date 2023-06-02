package scenarios

import zio.{
  Duration,
  Schedule,
  Unsafe,
  ZIO,
  ZIOAppDefault,
  ZLayer,
  durationInt
}
import zio.Console.printLine

import scala.concurrent.TimeoutException
import time.scheduledValues
import izumi.reflect.Tag

case class TempSense(
    z: ZIO[
      Any,
      HardwareFailure,
      ZIO[Any, TimeoutException, Degrees]
    ]
)

case class SecuritySystemX(
    motionDetector: MotionDetector,
    thermalDetectorX: ThermalDetectorX,
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
  val s: zio.ZLayer[
    Any,
    Nothing,
    scenarios.TempSense
  ] =
    SensorData.live[Degrees, TempSense](
      x => TempSense(x),
      (1.seconds, Degrees(71)),
      (2.seconds, Degrees(70))
    )

  val accessMotionDetector: ZIO[
    scenarios.MotionDetector,
    scenarios.HardwareFailure,
    scenarios.Pixels
  ] = ZIO.serviceWithZIO(_.amountOfMotion())

  def securityLoop(
      amountOfHeatGenerator: ZIO[
        Any,
        scala.concurrent.TimeoutException |
          scenarios.HardwareFailure,
        scenarios.Degrees
      ],
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
    for
      amountOfHeat <- amountOfHeatGenerator
      noise        <- acousticDetector
      _ <-
        ZIO.debug(
          s"Heat: $amountOfHeat  Motion: $amountOfMotion  Noise: $noise"
        )
      securityResponse =
        determineResponse(
          amountOfMotion,
          amountOfHeat,
          noise
        )
      _ <-
        securityResponse match
          case Relax =>
            ZIO.debug("No need to panic")
          case LowBeep =>
            SirenX.lowBeep
          case LoudSiren =>
            SirenX.loudSiren
    yield ()

  def shouldAlertServices[
      T
        <: MotionDetector & ThermalDetectorX &
          SirenX & AcousticDetectorX
  ](): ZIO[
    T,
    scenarios.HardwareFailure | TimeoutException,
    String
  ] =
    for
      amountOfMotion <-
        MotionDetector
          .acquireMotionMeasurementSource()
      amountOfHeatGenerator <-
        ThermalDetectorX
          .acquireHeatMeasurementSource
      acousticDetector <-
        AcousticDetectorX.acquireDetector
      _ <-
        securityLoop(
          amountOfHeatGenerator,
          amountOfMotion,
          acousticDetector
        ).repeat(
          Schedule.recurs(5) &&
            Schedule.spaced(1.seconds)
        )
    yield "Fin"

  def shouldTrigger(
      amountOfMotion: Pixels,
      amountOfHeat: Degrees
  ): Boolean =
    amountOfMotion.value > 10 &&
      amountOfHeat.value > 95

  def determineResponse(
      amountOfMotion: Pixels,
      amountOfHeat: Degrees,
      noise: Decibels
  ): SecurityResponse =
    val numberOfAlerts =
      List(
        amountOfMotion.value > 50,
        amountOfHeat.value > 95,
        noise.value > 15
      ).filter(_ == true).length

    if (numberOfAlerts == 0)
      Relax
    else if (numberOfAlerts == 1)
      LowBeep
    else
      LoudSiren
  end determineResponse

  def determineBreaches(
      amountOfMotion: Pixels,
      amountOfHeat: Degrees,
      noise: Decibels
  ): Set[SecurityBreach] =
    List(
      Option.when(amountOfMotion.value > 50)(
        SignificantMotion
      ),
      Option.when(
        amountOfHeat.value > 95 &&
          amountOfHeat.value < 200
      )(BodyHeat),
      Option
        .when(amountOfHeat.value >= 200)(Fire),
      Option.when(noise.value > 15)(LoudNoise)
    ).flatten.toSet

end SecuritySystem

trait SecurityBreach
object BodyHeat          extends SecurityBreach
object Fire              extends SecurityBreach
object LoudNoise         extends SecurityBreach
object SignificantMotion extends SecurityBreach

trait SecurityResponse
object Relax     extends SecurityResponse
object LowBeep   extends SecurityResponse
object LoudSiren extends SecurityResponse

trait HardwareFailure

case class Decibels(value: Int)
case class Degrees(value: Int)
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

trait ThermalDetectorX:
  def heatMeasurementSource()
      : ZIO[Any, Nothing, ZIO[
        Any,
        TimeoutException |
          scenarios.HardwareFailure,
        Degrees
      ]]

trait ThermalDetectorY:
  def heatMeasurement(): ZIO[
    Any,
    TimeoutException | scenarios.HardwareFailure,
    Degrees
  ]

object ThermalDetectorY:

  def apply(
      value: (Duration, Degrees),
      values: (Duration, Degrees)*
  ): ZLayer[Any, Nothing, ThermalDetectorY] =
    ZLayer.fromZIO(
      for thermalDetectorValues <-
          scheduledValues(value, values*)
      yield new ThermalDetectorY:
        override def heatMeasurement(): ZIO[
          Any,
          TimeoutException |
            scenarios.HardwareFailure,
          Degrees
        ] = thermalDetectorValues
    )

object ThermalDetectorX:

  def apply(
      value: (Duration, Degrees),
      values: (Duration, Degrees)*
  ): ZLayer[Any, Nothing, ThermalDetectorX] =
    ZLayer.succeed(
      // that same service we wrote above
      new ThermalDetectorX:
        override def heatMeasurementSource()
            : ZIO[Any, Nothing, ZIO[
              Any,
              TimeoutException |
                scenarios.HardwareFailure,
              Degrees
            ]] = scheduledValues(value, values*)
    )

  // This is preeeetty gnarly. How can we
  // improve?
  def acquireHeatMeasurementSource[
      T <: scenarios.ThermalDetectorX: Tag
  ]: ZIO[T, Nothing, ZIO[
    Any,
    scala.concurrent.TimeoutException |
      scenarios.HardwareFailure,
    scenarios.Degrees
  ]] =
    ZIO.serviceWithZIO[ThermalDetectorX](
      _.heatMeasurementSource()
    )

end ThermalDetectorX

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
  ] = ZIO.serviceWith(_.lowBeep())

  val loudSiren: ZIO[
    SirenX,
    scenarios.HardwareFailure,
    Unit
  ] = ZIO.serviceWith(_.loudSiren())

end SirenX

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
