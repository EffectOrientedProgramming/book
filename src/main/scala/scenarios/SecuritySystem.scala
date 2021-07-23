package scenarios

import zio.{Has, ZIO, ZLayer}
import zio.console.putStrLn

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

  val fullLayer: ZLayer[Any, Nothing, zio.Has[
    scenarios.MotionDetector.ServiceX
  ] & zio.Has[scenarios.ThermalDetector.Service] & Has[Siren.ServiceX]] =
    MotionDetector.live ++
      ThermalDetector.live ++ Siren.live

  def shouldAlertServices(): ZIO[Has[
    MotionDetector.ServiceX
  ] & Has[ThermalDetector.Service] & Has[Siren.ServiceX], scenarios.HardwareFailure, String] =
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
            amountOfHeat <-
              thermalDetector.amountOfHeat()
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

  def shouldAlert(): ZIO[Has[
    MotionDetector.ServiceX
  ] & Has[ThermalDetector.Service], scenarios.HardwareFailure, String] =
    for
      motionDetector <-
        ZIO.service[MotionDetector.ServiceX]
      amountOfMotion <-
        motionDetector.amountOfMotion()
      thermalDetector <-
        ZIO.service[ThermalDetector.Service]
      amountOfHeat <-
        thermalDetector.amountOfHeat()
      _ <-
        ZIO.succeed(
          println(
            s"Heat: $amountOfHeat  Motion: $amountOfMotion"
          )
        )
    yield "Fin"
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
          .provideLayer(SecuritySystem.fullLayer)
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
    def amountOfHeat()
        : ZIO[Any, HardwareFailure, Degrees]

  val live: ZLayer[Any, Nothing, Has[
    ThermalDetector.Service
  ]] =
    ZLayer.succeed(
      // that same service we wrote above
      new Service:
        var temperatures = List(72, 73, 98)

        def amountOfHeat(): ZIO[
          Any,
          HardwareFailure,
          Degrees
        ] =
          val (curTemp :: remainingTemps) =
            temperatures
          temperatures = remainingTemps
          ZIO.succeed(Degrees(curTemp))
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
