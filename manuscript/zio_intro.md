## zio_intro

 

### experiments/src/main/scala/zio_intro/AuthenticationFlow.scala
```scala
package zio_intro

import zio.{ZIO, ZIOAppDefault}

object AuthenticationFlow extends ZIOAppDefault:
  val activeUsers
      : ZIO[Any, DiskError, List[UserName]] = ???

  val user: ZIO[Any, Nothing, UserName] = ???

  def authenticateUser(
      users: List[UserName],
      currentUser: UserName
  ): ZIO[
    Any,
    UnauthenticatedUser,
    AuthenticatedUser
  ] = ???

  val fullAuthenticationProcess: ZIO[
    Any,
    DiskError | UnauthenticatedUser,
    AuthenticatedUser
  ] =
    for
      users       <- activeUsers
      currentUser <- user
      authenticatedUser <-
        authenticateUser(users, currentUser)
    yield authenticatedUser

  def run =
    fullAuthenticationProcess.orDieWith(error =>
      new Exception("Unhandled error: " + error)
    )
end AuthenticationFlow

trait UserName
case class FileSystem()
trait DiskError
trait EnvironmentVariableNotFound
case class UnauthenticatedUser(msg: String)
case class AuthenticatedUser(userName: UserName)

```


### experiments/src/main/scala/zio_intro/FirstMeaningfulExample.scala
```scala
package zio_intro

import zio.{Clock, ZIO, ZIOAppDefault, System}
import zio.Console.{readLine, printLine}

object HelloWorld extends ZIOAppDefault:
  def run = printLine("Hello World")

object FirstMeaningfulExample
    extends ZIOAppDefault:
  def run =
    for
      _    <- printLine("Give us your name:")
      name <- readLine
      _    <- printLine(s"$name")
    yield ()

```


### experiments/src/main/scala/zio_intro/OpeningHook.scala
```scala
package zio_intro

import zio.*
import zio.direct.*
import zio.Schedule.*

object DatabaseError
object TimeoutError

val worksFirstTime = Scenario.WorksFirstTime

val doesNotWork = Scenario.DoesNotWork

val firstIsSlow =
  Unsafe.unsafe { implicit unsafe =>
    Scenario.FirstIsSlow(
      Runtime
        .default
        .unsafe
        .run(Ref.make(0))
        .getOrThrow()
    )
  }

val doesNotWorkFirstTime =
  Unsafe.unsafe { implicit unsafe =>
    Scenario.WorksOnTry(
      1,
      Runtime
        .default
        .unsafe
        .run(Ref.make(0))
        .getOrThrow()
    )
  }

enum Scenario:
  case WorksFirstTime
  case DoesNotWork
  case FirstIsSlow(ref: Ref[Int])
  case WorksOnTry(attempts: Int, ref: Ref[Int])

def saveUser(
    username: String,
    hiddenScenario: Scenario = worksFirstTime
): ZIO[Any, DatabaseError.type, String] =
  val succeed = ZIO.succeed("User saved")
  val fail =
    ZIO
      .fail(DatabaseError)
      .tapError { _ =>
        Console
          .printLineError("Database Error")
          .orDie
      }

  defer {
    hiddenScenario match
      case Scenario.WorksFirstTime =>
        succeed.run
      case Scenario.DoesNotWork =>
        fail.run

      case Scenario.FirstIsSlow(ref) =>
        val numCalls =
          ref.getAndUpdate(_ + 1).run
        if numCalls == 0 then
          ZIO.never.run
        else
          Console
            .printLineError("Database Timeout")
            .orDie
            .run
          succeed.run

      case Scenario.WorksOnTry(attempts, ref) =>
        val numCalls =
          ref.getAndUpdate(_ + 1).run
        if numCalls == attempts then
          succeed.run
        else
          fail.run
  }
end saveUser

def sendToManualQueue(
    username: String
): ZIO[Any, TimeoutError.type, String] =
  ZIO.succeed("User sent to manual setup queue")

def userSignedUp(username: String) =
  ZIO.succeed("Analytics sent")

// works
object One extends ZIOAppDefault:
  override def run = saveUser("mrsdavis").debug

// fails
object Two extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWork)
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// fails every time - with retry
object Three extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWork)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// fails first time - with retry
object Four extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWorkFirstTime)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// first is slow - with timeout and retry
object Five extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", firstIsSlow)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// fails - with retry and fallback
object Six extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWork)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElse(sendToManualQueue("mrsdavis"))
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// concurrently save & send analytics
object Seven extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis")
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElse(sendToManualQueue("mrsdavis"))
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug
      .zipPar(userSignedUp("mrsdavis").debug)

```


### experiments/src/main/scala/zio_intro/ProgressBar.scala
```scala
package zio_intro

import zio.{Ref, *}

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

```


