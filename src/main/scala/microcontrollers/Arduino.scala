package microcontrollers

import zio.console.{
  Console,
  getStrLn,
  putStrLn
}
import zio.clock.currentTime
import zio.{
  Fiber,
  IO,
  Runtime,
  Schedule,
  UIO,
  ZIO,
  URIO,
  ZLayer,
  Ref
}
import zio.duration._
import java.util.concurrent.TimeUnit

case class DigitalPin(active: Boolean)

case class Arduino(pin1: DigitalPin) {

  def passSignalToLight() =
    if (pin1.active)
      ZIO.succeed(
        "Sending current to lightbulb"
      )
    else
      ZIO.succeed("Leaving the lights off")
}

object MicroControllerExample extends zio.App {

  def turnOnPinAtRightTime() = ???

  def loopLogic(
      startTime: Long,
      arduino: Ref[Arduino]
  ) =
    for
      inSeconds <- currentTime(
        TimeUnit.SECONDS
      )
      originalArduino <- arduino.get
      originalLightStatus <- originalArduino
        .passSignalToLight()
      signalOnPin =
        (inSeconds - startTime) % 6 > 3
      _ <- arduino.set(
        Arduino(DigitalPin(signalOnPin))
      )
      updatedArduino <- arduino.get
      updatedLightStatus <- updatedArduino
        .passSignalToLight()
      _ <-
        if (
          originalLightStatus != updatedLightStatus
        )
          putStrLn(updatedLightStatus)
        else
          ZIO.succeed(1)
    yield ()

  def run(
      args: List[String]
  ) = //Use App's run function
    val logic =
      for
        arduino <- Ref.make(
          Arduino(pin1 = DigitalPin(false))
        )
        inSeconds <- currentTime(
          TimeUnit.SECONDS
        )
        _ <- putStrLn(
          "startTime: " + inSeconds
        )
        _ <- loopLogic(inSeconds, arduino)
          .repeat(
            Schedule.recurs(60) && Schedule
              .spaced(100.milliseconds)
          )
      yield ()
    logic.exitCode
}
