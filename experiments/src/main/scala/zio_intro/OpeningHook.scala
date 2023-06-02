package zio_intro

import zio.*
import zio.direct.*
import zio.Schedule.*

object DatabaseError
object TimeoutError

val worksFirstTime = Scenario.WorksFirstTime

val doesNotWork = Scenario.DoesNotWork

val firstIsSlow = Unsafe.unsafe { implicit unsafe =>
  Scenario.FirstIsSlow(
    Runtime.default.unsafe.run(Ref.make(0)).getOrThrow()
  )
}

enum Scenario:
  case WorksFirstTime
  case DoesNotWork
  case FirstIsSlow(ref: Ref[Int])
  case WorksOnTry(attempts: Int, ref: Ref[Int])

def saveUser(username: String, hiddenScenario: Scenario = worksFirstTime): ZIO[Any, DatabaseError.type, String] =
  defer {
    hiddenScenario match
      case Scenario.WorksFirstTime =>
        ZIO.succeed("User saved").run
      case Scenario.DoesNotWork =>
        ZIO.fail(DatabaseError).tapError { _ =>
          Console.printLineError("Database Error").orDie
        }.run

      case Scenario.FirstIsSlow(ref) =>
        val numCalls = ref.getAndUpdate(_ + 1).run
        if numCalls == 0 then
          ZIO.never.run
        else
          Console.printLineError("Database Timeout").orDie.run
          ZIO.succeed("User saved").run

      case Scenario.WorksOnTry(attempts, ref) => ???
  }

def sendToManualQueue(username: String): ZIO[Any, TimeoutError.type, String] =
  ZIO.succeed("User sent to manual setup queue")


object First extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis")
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed("ERROR: User could not be saved")
      .debug

object Second extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWork)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed("ERROR: User could not be saved")
      .debug

object Third extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", firstIsSlow)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed("ERROR: User could not be saved")
      .debug

// todo
object Fourth extends ZIOAppDefault:
  override def run =
    val username = "mrsdavis"
    saveUser(username)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElse(sendToManualQueue(username))
      .orElseSucceed("ERROR: User could not be saved")
      .debug
