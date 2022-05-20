package microcontrollers

import zio.Console.{printLine, readLine}
import zio.{
  Clock,
  Console,
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
    for
      inSeconds <- currentTime(TimeUnit.SECONDS)
      originalArduino <- arduino.get
      originalLightStatus <-
        originalArduino.passSignalToLight()
      signalOnPin =
        turnOnPinAtRightTime(
          inSeconds,
          startTime
        )
      _ <- arduino.set(Arduino(signalOnPin))
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

  def run =
    for
      arduino <-
        Ref.make(Arduino(pin1 = DigitalPin.OFF))
      inSeconds <- currentTime(TimeUnit.SECONDS)
      _ <-
        loopLogic(inSeconds, arduino).repeat(
          // Can we calculate how long this is
          // using Schedule APIs?
          Schedule.recurs(60) &&
            Schedule.spaced(100.milliseconds)
        )
    yield ()
end MicroControllerExample
