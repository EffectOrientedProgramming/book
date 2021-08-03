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
  ] & zio.Has[scenarios.ThermalDetectorX] & Has[Siren.ServiceX]] =
    MotionDetector.live ++
      ThermalDetectorX.live(
        (1.seconds, Degrees(71)),
        (2.seconds, Degrees(70)),
        (3.seconds, Degrees(98))
      ) ++ Siren.live // ++ s
//    ++
//    SensorData
// .liveS[Degrees]((1.seconds, Degrees(71)))
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
      siren: Siren.ServiceX
  ): ZIO[Has[
    Clock
  ], scala.concurrent.TimeoutException | HardwareFailure, Unit] =
    for
      amountOfHeat <- amountOfHeatGenerator
      _ <-
        ZIO.debug(
          s"Heat: $amountOfHeat  Motion: $amountOfMotion"
        )
      _ <-
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
  ] & Has[ThermalDetectorX] & Has[Siren.ServiceX] & Has[Clock], scenarios.HardwareFailure | TimeoutException, String] =
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
          _ <-
            securityLoop(
              amountOfHeatGenerator,
              amountOfMotion,
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
    amountOfMotion.value > 50 &&
      amountOfHeat.value > 95

end SecuritySystem

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
      ZIO.succeed(Pixels(100))

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

  def live(
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
            ]] =
          Scheduled2
            .scheduledValues(value, values*)
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
      c(
        Scheduled2
          .scheduledValues[T](value, values*)
      )
    )

  def liveS[T](
      value: (Duration, T),
      values: (Duration, T)*
  ): ZLayer[Any, Nothing, Has[SensorD[T]]] =
    ZLayer.succeed(
      // that same service we wrote above
      SensorD(
        Scheduled2
          .scheduledValues[T](value, values*)
      )
    )
end SensorData
