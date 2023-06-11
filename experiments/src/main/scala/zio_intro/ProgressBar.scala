package zio_intro

import zio.*
import zio.direct.*

import zio.Console.printLine

import java.util.concurrent.TimeUnit

import scala.io.AnsiColor.*

val saveCursorPosition = Console.print("\u001b7")
val loadCursorPosition = Console.print("\u001b8")

def progressBar(
    length: RuntimeFlags,
    label: String = ""
): IO[Any, Unit] =
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
    defer {
      val currentTime =
        Clock.currentTime(TimeUnit.SECONDS).run
      renderRemainingTime(currentTime)
        .repeat(Schedule.recurs(10))
        .run
    }

  def renderRemainingTime(startTime: Long) =
    defer {
      val currentTime =
        Clock.currentTime(TimeUnit.SECONDS).run
      val timeElapsed = (currentTime - startTime)
        .toInt
      // NOTE: You can only reset the cursor //
      // position once in a single SBT session
      saveCursorPosition.run
      val timeRemaining = 10 - timeElapsed
      Console.print(
        s"${BOLD}$timeRemaining seconds remaining ${RESET}"
      ).run
      progressBar(timeRemaining).run
      ZIO.sleep(1.seconds).run
      loadCursorPosition.run
    }

  def run = renderCurrentTime
end ClockAndConsole

object ClockAndConsoleImproved
    extends ZIOAppDefault:
  val renderCurrentTime =
    defer {
      val currentTime =
        Clock.currentTime(TimeUnit.SECONDS).run
      val racer1 =
        LongRunningProcess(
          "Shtep",
          currentTime,
          3
        ).run
      val racer2 =
        LongRunningProcess("Zeb", currentTime, 5).run
      val raceFinished = Ref.make[Boolean](false).run
      val winnersName =
        (raceEntities(
          racer1.run,
          racer1.run,
          raceFinished
        ) zipParLeft
          monitoringLogic(
            racer1,
            racer2,
            raceFinished
          )).run
      printLine(s"\nWinner: $winnersName").run
    }

  def monitoringLogic(
      racer1: LongRunningProcess,
      racer2: LongRunningProcess,
      raceFinished: Ref[Boolean]
  ) =
    renderLoop(
      defer {
        val racer1status = racer1.status.get.run
        val racer2status = racer2.status.get.run
        progressBar(racer1status, racer1.name).run
        printLine("").run
        progressBar(racer2status, racer2.name).run
      }
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
    defer {
      saveCursorPosition.run
      drawFrame.run
      ZIO.sleep(1.second).run
      loadCursorPosition.run
    }

  def timer(startTime: Long, secondsToRun: Int) =
    defer {
      val currentTime =
        Clock.currentTime(TimeUnit.SECONDS).run
      val timeElapsed = (currentTime - startTime)
        .toInt
      Integer
        .max(secondsToRun - timeElapsed, 0)
    }

  object LongRunningProcess:
    def apply(
        name: String,
        startTime: Long,
        secondsToRun: Int
    ): ZIO[Any, Nothing, LongRunningProcess] =
      defer {
        val status = Ref.make[Int](4).run
        new LongRunningProcess(
          name,
          startTime,
          secondsToRun,
          status
        )
      }

  class LongRunningProcess(
      val name: String,
      startTime: Long,
      secondsToRun: Int,
      val status: Ref[Int]
  ):
    val loopAndCheck =
      defer {
        val timeLeft =
          timer(startTime, secondsToRun).run
        status.set(timeLeft).run
        timeLeft
      }

    val run: ZIO[Any, Nothing, String] =
      loopAndCheck
        .repeatUntil(_ == 0)
        .map(_ => name)

  def run = renderCurrentTime
end ClockAndConsoleImproved
