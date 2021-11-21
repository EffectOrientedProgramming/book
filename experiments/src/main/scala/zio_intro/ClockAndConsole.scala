package zio_intro

import zio.*
import zio.Console.printLine

import java.util.concurrent.TimeUnit
import io.AnsiColor._

object ClockAndConsole extends ZIOAppDefault:
  val renderCurrentTime =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      _ <-
        renderRemainingTime(currentTime)
          .repeat(Schedule.recurs(10))
    yield ()

  val saveCursorPosition =
    Console.print("\u001b7")
  val loadCursorPosition =
    Console.print("\u001b8")

  def renderRemainingTime(startTime: Long) =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime)
        .toInt
      timeRemaining = 10 - timeElapsed
      // NOTE: You can only reset the cursor
      // position once in a single SBT session
      _ <- saveCursorPosition
      _ <-
        Console.print(
          s"${BOLD}$timeRemaining seconds remaining ${RESET}"
        )
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
    Console.printLine(
      s"""${color}${" " * length}${RESET}"""
    )

  def run = renderCurrentTime
end ClockAndConsole

object ClockAndConsoleDifficultEffectManagement
    extends ZIOAppDefault:
  val renderCurrentTime =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      _ <-
        renderRemainingTime(currentTime)
          .repeat(Schedule.recurs(10))
      _ <-
        renderRemainingTime(
          Integer.max(currentTime.toInt - 5, 0)
        ).repeat(Schedule.recurs(10))
    yield ()

  val saveCursorPosition =
    Console.print("\u001b7")
  val loadCursorPosition =
    Console.print("\u001b8")

  def renderRemainingTime(startTime: Long) =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      timeElapsed = (currentTime - startTime)
        .toInt
      timeRemaining = 10 - timeElapsed
      // NOTE: You can only reset the cursor
      // position once in a single SBT session
      _ <- saveCursorPosition
      _ <-
        Console.print(
          s"${BOLD}$timeRemaining seconds remaining ${RESET}"
        )
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
    Console.printLine(
      s"""${color}${" " * length}${RESET}"""
    )

  def run = renderCurrentTime
end ClockAndConsoleDifficultEffectManagement

object ClockAndConsoleImproved
    extends ZIOAppDefault:
  val renderCurrentTime =
    for
      currentTime <-
        Clock.currentTime(TimeUnit.SECONDS)
      racer1 <- LoopingTimer(
        "Shtep",
        currentTime,
        3,
      )
      racer2 <- LoopingTimer(
        "Zeb",
        currentTime,
        5,
      )
      _ <-
      raceEntities(
        racer1.run,
        racer1.run
      ) zipPar
        loopLogic(
          racer1.status,
          racer2.status
        )
    yield ()

  def loopLogic(
      racer1timeRemaining: Ref[Int],
      racer2timeRemaining: Ref[Int]
  ) =
    renderLoop(
      for
        racer1status <- racer1timeRemaining.get
        racer2status <- racer2timeRemaining.get
        _            <- progressBar(racer1status)
        _            <- printLine("")
        _            <- progressBar(racer2status)
      yield ()
    ).repeat(Schedule.recurs(5))

  def raceEntities(
      racer1: ZIO[Clock, Nothing, String],
      racer2: ZIO[Clock, Nothing, String]
  ) =
    racer1.raceWith(racer2)(
      leftDone =
        (result, _) =>
          result match
            case zio.Exit.Success(success) =>
              ZIO
                .debug(s"$success won the race!")
            case zio.Exit.Failure(failure) =>
              ???
      ,
      rightDone =
        (result, _) =>
          result match
            case zio.Exit.Success(success) =>
              ZIO
                .debug(s"$success won the race!")
            case zio.Exit.Failure(failure) =>
              ???
    )

  def updateLogic(currentTime: Long) =
    for
      _ <- renderRemainingTime(currentTime)
      _ <- Console.printLine("")
      _ <-
        renderRemainingTime(
          currentTime.toInt - 5
        )
      _ <- ZIO.sleep(1.second)
    yield ()

  val saveCursorPosition =
    Console.print("\u001b7")
  val loadCursorPosition =
    Console.print("\u001b8")

  def renderLoop[T <: Console with Clock](
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
    
  object LoopingTimer:
    def apply(
               name: String,
               startTime: Long,
               secondsToRun: Int,
             ): ZIO[Any, Nothing, LoopingTimer] =
      for
        status <- Ref.make[Int](4)
      yield
        new LoopingTimer(
          name,
          startTime,
          secondsToRun,
          status
        )
        

  class LoopingTimer(
                    name: String,
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
      
    val run : ZIO[Clock, Nothing, String] =
      loopAndCheck.repeatUntil(_ == 0).map(_ => name)

  def loopingTimer(
      name: String,
      startTime: Long,
      secondsToRun: Int,
      status: Ref[Int]
  ): ZIO[Clock, Nothing, String] =
    (
      for
        timeLeft <-
          timer(startTime, secondsToRun)
        _ <- status.set(timeLeft)
      yield timeLeft
    ).repeatUntil(_ == 0).map(_ => name)

  def renderRemainingTime(startTime: Long) =
    for
      timeRemaining <- timer(startTime, 10)
      // NOTE: You can only reset the cursor
      // position once in a single SBT session
      _ <-
        Console.print(
          s"${BOLD}$timeRemaining seconds remaining ${RESET}"
        )
      _ <- progressBar(timeRemaining)
    yield ()

  def progressBar(length: Int) =
    val color =
      if (length > 3)
        GREEN_B
      else
        RED_B
    Console.print(
      s"""${color}${" " * length}${RESET}"""
    )

  def run = renderCurrentTime
end ClockAndConsoleImproved
