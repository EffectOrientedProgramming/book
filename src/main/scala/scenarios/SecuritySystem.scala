package scenarios

import zio.{Has, ZIO, ZLayer}
import zio.clock.Clock
import zio.duration.Duration
import zio.console.putStrLn
import zio.duration.durationInt
import scala.concurrent.TimeoutException

/** Situations: Security System: Should monitor
  *   - Motion
  *   - Heat/Infrared
  *   - Sound Should alert by:
  *   - Quiet, local beep
  *   - Loud Local Siren
  *   - Ping security company
  *   - Notify police
  *
  * TODO Investigate services that enable timed,
  * chunked results. eg:
  *   - 70 degrees for 5 seconds
  *   - 71 degrees for 10 seconds
  *   - 98 degrees for 3 seconds
  */
object SecuritySystem:

  val fullLayer: ZLayer[Any, Nothing, zio.Has[
    scenarios.MotionDetector.ServiceX
  ] & zio.Has[scenarios.ThermalDetector.Service] & Has[Siren.ServiceX]] =
    MotionDetector.live ++
      ThermalDetector
        .live((10.seconds, Degrees(71))) ++
      Siren.live

  def shouldAlertServices(): ZIO[Has[
    MotionDetector.ServiceX
  ] & Has[ThermalDetector.Service] & Has[Siren.ServiceX] & Clock, scenarios.HardwareFailure | TimeoutException, String] =
    ZIO
      .services[
        MotionDetector.ServiceX,
        ThermalDetector.Service,
        Siren.ServiceX
      ]
      .flatMap {
        (
            motionDetector,
            thermalDetector,
            siren
        ) =>
          for
            amountOfMotion <-
              motionDetector.amountOfMotion()
            amountOfHeatGenerator <-
              thermalDetector.amountOfHeat()
            amountOfHeat <- amountOfHeatGenerator

            _ <-
              zprintln(
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
                zprintln("No need to panic")
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
          .repeatN(2)
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

object MotionDetector:
  trait ServiceX:
    def amountOfMotion()
        : ZIO[Any, HardwareFailure, Pixels]

  val live: ZLayer[Any, Nothing, Has[
    MotionDetector.ServiceX
  ]] =
    ZLayer.succeed(
      // that same service we wrote above
      new ServiceX:

        def amountOfMotion()
            : ZIO[Any, HardwareFailure, Pixels] =
          ZIO.succeed(Pixels(100))
    )
end MotionDetector

object ThermalDetector:
  trait Service:
    def amountOfHeat(): ZIO[
      zio.clock.Clock,
      HardwareFailure,
      ZIO[Clock, TimeoutException, Degrees]
    ]

  def live(
      value: (Duration, Degrees),
      values: (Duration, Degrees)*
  ): ZLayer[Any, Nothing, Has[
    ThermalDetector.Service
  ]] =
    ZLayer.succeed(
      // that same service we wrote above
      new Service:
        var temperatures = List(72, 73, 98)

        def amountOfHeat(): ZIO[
          zio.clock.Clock,
          HardwareFailure,
          ZIO[Clock, TimeoutException, Degrees]
        ] =
          val (curTemp :: remainingTemps) =
            temperatures
          temperatures = remainingTemps
          Scheduled2
            .scheduledValues(value, values*)
        end amountOfHeat
    )
end ThermalDetector

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
        ] = zprintln("beeeeeeeeeep")
    )
end Siren

def zprintln(
    output: String
): ZIO[Any, scenarios.HardwareFailure, Unit] =
  ZIO.succeed(println(output))
