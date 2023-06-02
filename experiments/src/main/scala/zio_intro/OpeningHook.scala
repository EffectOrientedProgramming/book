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

val doesNotWorkFirstTime = Unsafe.unsafe { implicit unsafe =>
  Scenario.WorksOnTry(
    1,
    Runtime.default.unsafe.run(Ref.make(0)).getOrThrow()
  )
}

enum Scenario:
  case WorksFirstTime
  case DoesNotWork
  case FirstIsSlow(ref: Ref[Int])
  case WorksOnTry(attempts: Int, ref: Ref[Int])

def saveUser(username: String, hiddenScenario: Scenario = worksFirstTime): ZIO[Any, DatabaseError.type, String] =
  val succeed = ZIO.succeed("User saved")
  val fail = ZIO.fail(DatabaseError).tapError { _ =>
    Console.printLineError("Database Error").orDie
  }

  defer {
    hiddenScenario match
      case Scenario.WorksFirstTime =>
        succeed.run
      case Scenario.DoesNotWork =>
        fail.run

      case Scenario.FirstIsSlow(ref) =>
        val numCalls = ref.getAndUpdate(_ + 1).run
        if numCalls == 0 then
          ZIO.never.run
        else
          Console.printLineError("Database Timeout").orDie.run
          succeed.run

      case Scenario.WorksOnTry(attempts, ref) =>
        val numCalls = ref.getAndUpdate(_ + 1).run
        if numCalls == attempts then
          succeed.run
        else
          fail.run
  }

def sendToManualQueue(username: String): ZIO[Any, TimeoutError.type, String] =
  ZIO.succeed("User sent to manual setup queue")

def userSignedUp(username: String) =
  ZIO.succeed("Analytics sent")


// works
object One extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis")
      .debug

// fails
object Two extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWork)
      .orElseSucceed("ERROR: User could not be saved")
      .debug

// fails every time - with retry
object Three extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWork)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed("ERROR: User could not be saved")
      .debug

// fails first time - with retry
object Four extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWorkFirstTime)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed("ERROR: User could not be saved")
      .debug

// first is slow - with timeout and retry
object Five extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", firstIsSlow)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed("ERROR: User could not be saved")
      .debug

// fails - with retry and fallback
object Six extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", doesNotWork)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElse(sendToManualQueue("mrsdavis"))
      .orElseSucceed("ERROR: User could not be saved")
      .debug

// concurrently save & send analytics
object Seven extends ZIOAppDefault:
  override def run =
    val sendAnalytics = userSignedUp("mrsdavis").debug

    saveUser("mrsdavis")
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElse(sendToManualQueue("mrsdavis"))
      .orElseSucceed("ERROR: User could not be saved")
      .debug
      .zipPar(sendAnalytics)
