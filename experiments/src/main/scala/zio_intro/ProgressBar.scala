package zio_intro

import zio.{Ref, *}

import zio.Console.printLine

import java.util.concurrent.TimeUnit

import scala.io.AnsiColor.*

val saveCursorPosition =
  Console.print("\u001b7")
val loadCursorPosition =
  Console.print("\u001b8")


def progressBar(length: RuntimeFlags, label: String = ""): IO[Any, Unit] =
  val barColor =
    if (length > 3)
      GREEN_B
    else
      RED_B
  Console.print(
    s"""$label$barColor${" " * length}$RESET"""
  )

object ClockAndConsole extends ZIOAppDefault:
  val renderCurrentTime =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      _ <-
        renderRemainingTime(currentTime)
          .repeat(Schedule.recurs(10))
    yield ()

  def renderRemainingTime(startTime: Long) =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime)
        .toInt
      // NOTE: You can only reset the cursor //
      // position once in a single SBT session
      _ <- saveCursorPosition
      timeRemaining = 10 - timeElapsed
      _ <-
        Console.print(
          s"${BOLD}$timeRemaining seconds remaining ${RESET}"
        )
      _ <- progressBar(timeRemaining)
      _ <- ZIO.sleep(1.seconds)
      _ <- loadCursorPosition
    yield ()

  def run = renderCurrentTime
end ClockAndConsole

object ClockAndConsoleImproved
    extends ZIOAppDefault:
  val renderCurrentTime =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      racer1 <-
        LongRunningProcess(
          "Shtep",
          currentTime,
          3
        )
      racer2 <-
        LongRunningProcess("Zeb", currentTime, 5)
      raceFinished <- Ref.make[Boolean](false)
      winnersName <-
      raceEntities(
        racer1.run,
        racer1.run,
        raceFinished
      ) zipParLeft
        monitoringLogic(
          racer1,
          racer2,
          raceFinished
        )
      _ <- printLine(s"\nWinner: $winnersName")
    yield ()

  def monitoringLogic(
      racer1: LongRunningProcess,
      racer2: LongRunningProcess,
      raceFinished: Ref[Boolean]
  ) =
    renderLoop(
      for
        racer1status <- racer1.status.get
        racer2status <- racer2.status.get
        _ <-
          progressBar(racer1status, racer1.name)
        _ <- printLine("")
        _ <-
          progressBar(racer2status, racer2.name)
      yield ()
    ).repeatWhileZIO(_ => raceFinished.get)

  def raceEntities(
      racer1: ZIO[Any, Nothing, String],
      racer2: ZIO[Any, Nothing, String],
      raceFinished: Ref[Boolean]
  ): ZIO[Any, Nothing, String] =
    racer1
//      .race(racer2)
      .flatMap { success =>
        raceFinished.set(true) *>
          ZIO.succeed(success)
      }

  def renderLoop[T](
      drawFrame: ZIO[T, Any, Unit]
  ) =
    for
      _ <- saveCursorPosition
      _ <- drawFrame
      _ <- ZIO.sleep(1.second)
      _ <- loadCursorPosition
    yield ()

  def timer(startTime: Long, secondsToRun: Int) =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime)
        .toInt
    yield Integer
      .max(secondsToRun - timeElapsed, 0)

  object LongRunningProcess:
    def apply(
        name: String,
        startTime: Long,
        secondsToRun: Int
    ): ZIO[Any, Nothing, LongRunningProcess] =
      for status <- Ref.make[Int](4)
      yield new LongRunningProcess(
        name,
        startTime,
        secondsToRun,
        status
      )

  class LongRunningProcess(
      val name: String,
      startTime: Long,
      secondsToRun: Int,
      val status: Ref[Int]
  ):
    val loopAndCheck =
      for
        timeLeft <-
          timer(startTime, secondsToRun)
        _ <- status.set(timeLeft)
      yield timeLeft

    val run: ZIO[Any, Nothing, String] =
      loopAndCheck
        .repeatUntil(_ == 0)
        .map(_ => name)

  def run = renderCurrentTime
end ClockAndConsoleImproved
