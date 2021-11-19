package zio_intro

import zio.*

import java.util.concurrent.TimeUnit
import io.AnsiColor._

object ClockAndConsole extends ZIOAppDefault {
  val renderCurrentTime =
    for
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      _ <- renderRemainingTime(currentTime).repeat(Schedule.recurs(10))
    yield ()
    
  val saveCursorPosition = Console.print("\u001b7")
  val loadCursorPosition = Console.print("\u001b8")
    
  def renderRemainingTime(startTime: Long) =
    for
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime).toInt
      timeRemaining = 10-timeElapsed
      // NOTE: You can only reset the cursor position once in a single SBT session
      _ <- saveCursorPosition
      _ <- Console.print(s"${BOLD}$timeRemaining seconds remaining ${RESET}")
      _ <- progressBar(timeRemaining)
      _ <- ZIO.sleep(1.seconds)
      _ <- loadCursorPosition
    yield ()
    
  def progressBar(length: Int) =
    val color =
      if (length > 3)
        GREEN_B
      else
        RED_B
    Console.printLine(s"""${color}${" " * length}${RESET}""")


  def run =
    renderCurrentTime

}

object ClockAndConsoleDifficultEffectManagement extends ZIOAppDefault {
  val renderCurrentTime =
    for
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      _ <- renderRemainingTime(currentTime).repeat(Schedule.recurs(10))
      _ <- renderRemainingTime(Integer.max(currentTime.toInt-5, 0)).repeat(Schedule.recurs(10))
    yield ()

  val saveCursorPosition = Console.print("\u001b7")
  val loadCursorPosition = Console.print("\u001b8")

  def renderRemainingTime(startTime: Long) =
    for
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime).toInt
      timeRemaining = 10-timeElapsed
      // NOTE: You can only reset the cursor position once in a single SBT session
      _ <- saveCursorPosition
      _ <- Console.print(s"${BOLD}$timeRemaining seconds remaining ${RESET}")
      _ <- progressBar(timeRemaining)
      _ <- ZIO.sleep(1.seconds)
      _ <- loadCursorPosition
    yield ()

  def progressBar(length: Int) =
    val color =
      if (length > 3)
        GREEN_B
      else
        RED_B
    Console.printLine(s"""${color}${" " * length}${RESET}""")


  def run =
    renderCurrentTime

}

object ClockAndConsoleImproved extends ZIOAppDefault {
  val renderCurrentTime =
    for
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      _ <- renderLoop(updateLogic(currentTime))
            .repeat(Schedule.recurs(10))
    yield ()
  
  def updateLogic(currentTime: Long) =
    for
      _ <- renderRemainingTime(currentTime)
      _ <- Console.printLine("")
      _ <- renderRemainingTime(currentTime.toInt-5)
      _ <- ZIO.sleep(1.second)
    yield ()

  val saveCursorPosition = Console.print("\u001b7")
  val loadCursorPosition = Console.print("\u001b8")
  
  def renderLoop[T<:Has[Console]](drawFrame: ZIO[T, Any, Unit]) =
    for
      _ <- saveCursorPosition
      _ <- drawFrame
      _ <- loadCursorPosition
    yield ()

  def renderRemainingTime(startTime: Long) =
    for
      currentTime <- Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime).toInt
      timeRemaining = Integer.max(10-timeElapsed, 0)
      // NOTE: You can only reset the cursor position once in a single SBT session
      _ <- Console.print(s"${BOLD}$timeRemaining seconds remaining ${RESET}")
      _ <- progressBar(timeRemaining)
    yield ()

  def progressBar(length: Int) =
    val color =
      if (length > 3)
        GREEN_B
      else
        RED_B
    Console.print(s"""${color}${" " * length}${RESET}""")


  def run =
    renderCurrentTime

}
