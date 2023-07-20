package microcontrollers

import zio.Console.{printLine, readLine}
import zio.Clock.{currentTime, instant}
import zio.Duration.*

import java.io.IOException
import java.util.concurrent.TimeUnit

case class DigitalPin private (active: Boolean)

object DigitalPin:
  object ON  extends DigitalPin(true)
  object OFF extends DigitalPin(false)

case class Arduino(pin1: DigitalPin):

  def passSignalToLight() =
    if (pin1.active)
      ZIO
        .succeed("Sending current to light bulb")
    else
      ZIO.succeed("Leaving the lights off")

object MicroControllerExample
    extends zio.ZIOAppDefault:

  def turnOnPinAtRightTime(
      inSeconds: Long,
      startTime: Long
  ) =
    if ((inSeconds - startTime) % 4 > 2)
      DigitalPin.ON
    else
      DigitalPin.OFF

  def loopLogic(
      startTime: Long,
      arduino: Ref[Arduino]
  ): ZIO[Any, IOException, Unit] =
    defer {
      val inSeconds =
        currentTime(TimeUnit.SECONDS).run
      val originalArduino = arduino.get.run
      val originalLightStatus =
        originalArduino.passSignalToLight().run
      val signalOnPin =
        turnOnPinAtRightTime(
          inSeconds,
          startTime
        )
      arduino.set(Arduino(signalOnPin)).run
      val updatedArduino = arduino.get.run
      val updatedLightStatus =
        updatedArduino.passSignalToLight().run
      if (
        originalLightStatus != updatedLightStatus
      )
        printLine(updatedLightStatus).run
      else
        ZIO.unit.run
    }

  def run =
    defer {
      val arduino =
        Ref
          .make(Arduino(pin1 = DigitalPin.OFF))
          .run
      val inSeconds =
        currentTime(TimeUnit.SECONDS).run
      loopLogic(inSeconds, arduino)
        .repeat(
          // Can we calculate how long this is
          // using Schedule APIs?
          Schedule.recurs(60) &&
            Schedule.spaced(100.milliseconds)
        )
        .run
    }
end MicroControllerExample
