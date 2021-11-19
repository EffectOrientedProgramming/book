package zio_intro

import zio.*

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

object ClockAndConsole extends ZIOAppDefault {
  import io.AnsiColor._
  val londonOffset = ZoneOffset.ofHours(0)
  val renderCurrentTime =
    for {
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      _ <- renderRemainingTime(currentTime).repeat(Schedule.recurs(10))
    } yield ()
    
  val saveCursorPosition = Console.print("\u001b7")
  val loadCursorPosition = Console.print("\u001b8")
    
  def renderRemainingTime(startTime: Long) =
    for {
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime).toInt
      timeRemaining = 10-timeElapsed
      // NOTE: You can only reset the cursor position once in a single SBT session
      _ <- saveCursorPosition
      _ <- Console.print(s"${BOLD}$timeRemaining seconds remaining ${RESET}")
      _ <- progressBar(timeRemaining)
      _ <- ZIO.sleep(1.seconds)
      _ <- loadCursorPosition
    } yield ()
    
  def progressBar(length: Int) =
    val color =
      if (length > 3)
        GREEN_B
      else
        RED_B
    Console.printLine(s"""${color}${" " * length}${RESET}""")


  /*
    Ideas:
      Countdown timer with status bar
  */
    
  def run =
    renderCurrentTime


}