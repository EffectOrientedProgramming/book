package scenarios.microcontrollers

import zio.Console.{readLine, printLine}
import zio.Console
import zio.Clock.currentTime
import zio.{
  Fiber,
  IO,
  Ref,
  Runtime,
  Schedule,
  UIO,
  URIO,
  ZIO,
  ZLayer,
  durationInt
}
import zio.Duration._
import java.util.concurrent.TimeUnit

case class DigitalPin(active: Boolean)

case class Arduino(pin1: DigitalPin):

  def passSignalToLight() =
    if (pin1.active)
      ZIO.succeed("Sending current to lightbulb")
    else
      ZIO.succeed("Leaving the lights off")

object MicroControllerExample extends zio.App:

  def turnOnPinAtRightTime() = ???

  def loopLogic(
      startTime: Long,
      arduino: Ref[Arduino]
  ) =
    for
      inSeconds <- currentTime(TimeUnit.SECONDS)
      originalArduino <- arduino.get
      originalLightStatus <-
        originalArduino.passSignalToLight()
      signalOnPin =
        (inSeconds - startTime) % 6 > 3
      _ <-
        arduino
          .set(Arduino(DigitalPin(signalOnPin)))
      updatedArduino <- arduino.get
      updatedLightStatus <-
        updatedArduino.passSignalToLight()
      _ <-
        if (
          originalLightStatus !=
            updatedLightStatus
        )
          printLine(updatedLightStatus)
        else
          ZIO.succeed(1)
    yield ()

  def run(
      args: List[String]
  ) = // Use App's run function
    val logic =
      for
        arduino <-
          Ref.make(
            Arduino(pin1 = DigitalPin(false))
          )
        inSeconds <-
          currentTime(TimeUnit.SECONDS)
        _ <- printLine("startTime: " + inSeconds)
        _ <-
          loopLogic(inSeconds, arduino).repeat(
            Schedule.recurs(60) &&
              Schedule.spaced(100.milliseconds)
          )
      yield ()
    logic.exitCode
  end run
end MicroControllerExample
