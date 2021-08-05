package scenarios

import zio.{Has, ZIO, ZLayer, Layer}
import zio.Clock
import zio.Duration
import zio.Console.printLine
import zio.durationInt
import zio.Schedule
import scala.concurrent.TimeoutException

case class TempSense(
    z: ZIO[Has[Clock], HardwareFailure, ZIO[Has[
      Clock
    ], TimeoutException, Degrees]]
)

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
  val s: zio.ZLayer[Any, Nothing, zio.Has[
    scenarios.TempSense
  ]] =
    SensorData.live[Degrees, TempSense](
      x => TempSense(x),
      (1.seconds, Degrees(71)),
      (2.seconds, Degrees(70))
    )

  val fullLayer: ZLayer[Any, Nothing, zio.Has[
    scenarios.MotionDetector
  ] & zio.Has[scenarios.ThermalDetectorX] & Has[AcousticDetectorX] & Has[Siren.ServiceX]] =
    MotionDetector.live ++
      ThermalDetectorX(
        (1.seconds, Degrees(71)),
        (2.seconds, Degrees(70)),
        (2.seconds, Degrees(98))
      ) ++ Siren.live // ++ s
    ++
    AcousticDetectorX(
      (4.seconds, Decibels(11)),
      (1.seconds, Decibels(20))
    )
  end fullLayer

  val accessMotionDetector: ZIO[Has[
    scenarios.MotionDetector
  ], scenarios.HardwareFailure, scenarios.Pixels] =
    ZIO.accessZIO(_.get.amountOfMotion())

  def securityLoop(
      amountOfHeatGenerator: ZIO[Has[
        Clock
      ], scala.concurrent.TimeoutException | scenarios.HardwareFailure, scenarios.Degrees],
      amountOfMotion: Pixels,
      acousticDetector: ZIO[Has[
        Clock
      ], scala.concurrent.TimeoutException | scenarios.HardwareFailure, scenarios.Decibels],
      siren: Siren.ServiceX
  ): ZIO[Has[
    Clock
  ], scala.concurrent.TimeoutException | HardwareFailure, Unit] =
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
        // TODO Expand shouldTrigger result type
        // for more complex responses
        if shouldTrigger(
            amountOfMotion,
            amountOfHeat
          )
        then
          siren.lowBeep()
        else
          ZIO.debug("No need to panic")
    yield ()

  def shouldAlertServices(): ZIO[Has[
    MotionDetector
  ] & Has[ThermalDetectorX] & Has[Siren.ServiceX] & Has[AcousticDetectorX] & Has[Clock], scenarios.HardwareFailure | TimeoutException, String] =
    ZIO
      .service[Siren.ServiceX]
      .flatMap { siren =>
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
              acousticDetector,
              siren
            ).repeat(
              Schedule.recurs(5) &&
                Schedule.spaced(1.seconds)
            )
        yield "Fin"
      }

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

end SecuritySystem

trait SecurityResponse
object Relax     extends SecurityResponse
object LowBeep   extends SecurityResponse
object LoudSiren extends SecurityResponse

@main
def useSecuritySystem =
  import zio.Runtime.default.unsafeRun
  println(
    "Final result: " +
      unsafeRun(
        SecuritySystem
          .shouldAlertServices()
          .provideLayer(
            SecuritySystem.fullLayer ++
              Clock.live
          )
          .catchSome {
            case _: TimeoutException =>
              printLine(
                "Invalid Scenario. Ran out of sensor data."
              )
          }
      )
  )
end useSecuritySystem

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

  end LiveMotionDetector

  def acquireMotionMeasurementSource(): ZIO[Has[
    MotionDetector
  ], HardwareFailure, Pixels] =
    ZIO.serviceWith(_.amountOfMotion())

  val live: Layer[Nothing, Has[MotionDetector]] =
    ZLayer.succeed(LiveMotionDetector)

end MotionDetector

trait ThermalDetectorX:
  def heatMeasurementSource()
      : ZIO[Has[Clock], Nothing, ZIO[
        Has[Clock],
        TimeoutException |
          scenarios.HardwareFailure,
        Degrees
      ]]

object ThermalDetectorX:

  def apply(
      value: (Duration, Degrees),
      values: (Duration, Degrees)*
  ): ZLayer[Any, Nothing, Has[
    ThermalDetectorX
  ]] =
    ZLayer.succeed(
      // that same service we wrote above
      new ThermalDetectorX:
        override def heatMeasurementSource()
            : ZIO[Has[Clock], Nothing, ZIO[
              Has[Clock],
              TimeoutException |
                scenarios.HardwareFailure,
              Degrees
            ]] = scheduledValues(value, values*)
    )

  // This is preeeetty gnarly. How can we
  // improve?
  val acquireHeatMeasurementSource: ZIO[Has[
    scenarios.ThermalDetectorX
  ] & Has[Clock], Nothing, ZIO[
    Has[Clock],
    scala.concurrent.TimeoutException |
      scenarios.HardwareFailure,
    scenarios.Degrees
  ]] =
    ZIO.accessZIO[Has[
      scenarios.ThermalDetectorX
    ] & Has[Clock]](
      _.get[scenarios.ThermalDetectorX]
        .heatMeasurementSource()
    )

end ThermalDetectorX

trait AcousticDetectorX:
  def acquireDetector()
      : ZIO[Has[Clock], Nothing, ZIO[
        Has[Clock],
        TimeoutException |
          scenarios.HardwareFailure,
        Decibels
      ]]

object AcousticDetectorX:

  def apply(
      value: (Duration, Decibels),
      values: (Duration, Decibels)*
  ): ZLayer[Any, Nothing, Has[
    AcousticDetectorX
  ]] =
    ZLayer.succeed(
      // that same service we wrote above
      new AcousticDetectorX:
        override def acquireDetector()
            : ZIO[Has[Clock], Nothing, ZIO[
              Has[Clock],
              TimeoutException |
                scenarios.HardwareFailure,
              Decibels
            ]] = scheduledValues(value, values*)
    )

  // This is preeeetty gnarly. How can we
  // improve?
  val acquireDetector: ZIO[Has[
    scenarios.AcousticDetectorX
  ] & Has[Clock], Nothing, ZIO[
    Has[Clock],
    scala.concurrent.TimeoutException |
      scenarios.HardwareFailure,
    scenarios.Decibels
  ]] =
    ZIO.accessZIO[Has[
      scenarios.AcousticDetectorX
    ] & Has[Clock]](
      _.get[scenarios.AcousticDetectorX]
        .acquireDetector()
    )

end AcousticDetectorX

object Siren:
  trait ServiceX:
    def lowBeep(): ZIO[
      Any,
      scenarios.HardwareFailure,
      Unit
    ]

  val live: ZLayer[Any, Nothing, Has[
    Siren.ServiceX
  ]] =
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

class SensorD[T](
    z: ZIO[Has[Clock], HardwareFailure, ZIO[Has[
      Clock
    ], TimeoutException, T]]
)

// TODO Figure out how to use this
object SensorData:
  def live[T, Y](
      c: ZIO[
        Has[Clock],
        HardwareFailure,
        ZIO[Has[Clock], TimeoutException, T]
      ] => Y,
      value: (Duration, T),
      values: (Duration, T)*
  ): ZLayer[Any, Nothing, Has[Y]] =
    ZLayer.succeed(
      // that same service we wrote above
      c(scheduledValues[T](value, values*))
    )

  def liveS[T](
      value: (Duration, T),
      values: (Duration, T)*
  ): ZLayer[Any, Nothing, Has[SensorD[T]]] =
    ZLayer.succeed(
      // that same service we wrote above
      SensorD(scheduledValues[T](value, values*))
    )
end SensorData
