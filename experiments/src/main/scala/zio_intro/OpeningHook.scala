package zio_intro

import zio.Schedule.*

object DatabaseError
object TimeoutError

object HiddenPrelude:
  enum Scenario:
    case WorksFirstTime
    case NeverWorks
    case FirstIsSlow(ref: Ref[Int])
    case WorksOnTry(attempts: Int, ref: Ref[Int])
  object Scenario:
    val firstIsSlow =
      Unsafe.unsafe { implicit unsafe =>
        FirstIsSlow(
          Runtime
            .default
            .unsafe
            .run(Ref.make(0))
            .getOrThrow()
        )
      }

    val doesNotWorkFirstTime =
      Unsafe.unsafe { implicit unsafe =>
        WorksOnTry(
          1,
          Runtime
            .default
            .unsafe
            .run(Ref.make(0))
            .getOrThrow()
        )
      }
  end Scenario

  def saveUser(
      username: String,
      hiddenScenario: Scenario =
        Scenario.WorksFirstTime
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
        case Scenario.NeverWorks =>
          fail.run

        case scenario: Scenario.FirstIsSlow =>
          val numCalls =
            scenario.ref.getAndUpdate(_ + 1).run
          if numCalls == 0 then
            ZIO.never.run
          else
            Console
              .printLineError("Database Timeout")
              .orDie
              .run
            succeed.run

        case Scenario
              .WorksOnTry(attempts, ref) =>
          val numCalls =
            ref.getAndUpdate(_ + 1).run
          if numCalls == attempts then
            succeed.run
          else
            fail.run
    }.onInterrupt(
      ZIO.debug("Interrupting slow request")
    )
  end saveUser

  def sendToManualQueue(
      username: String
  ): ZIO[Any, TimeoutError.type, String] =
    ZIO
      .succeed("User sent to manual setup queue")

  def userSignupInitiated(username: String) =
    ZIO.succeed(
      "Analytics sent for signup initiation"
    )

  def userSignupSucceeded(
      username: String,
      success: String
  ) =
    ZIO
      .succeed(
        "Analytics sent for signup completion"
      )
      .delay(1.second)
      .debug
      .fork
      .uninterruptible

  def userSignUpFailed(
      username: String,
      error: Any
  ) =
    ZIO
      .succeed(
        "Analytics sent for signup failure"
      )
      .delay(1.second)
      .debug
      .fork
      .uninterruptible
end HiddenPrelude

import HiddenPrelude.*

// works
object One extends ZIOAppDefault:
  override def run = saveUser("mrsdavis").debug

// fails
object Two extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", Scenario.NeverWorks)
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// fails first time - with retry
object Three extends ZIOAppDefault:
  override def run =
    saveUser(
      "mrsdavis",
      Scenario.doesNotWorkFirstTime
    ).retry(recurs(3) && spaced(1.second))
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// fails every time - with retry
object Four extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", Scenario.NeverWorks)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed(
        "ERROR: User could not be saved, despite multiple attempts"
      )
      .debug

// first is slow - with timeout and retry
object Five extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", Scenario.firstIsSlow)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug

// fails - with retry and fallback
object Six extends ZIOAppDefault:
  override def run =
    saveUser("mrsdavis", Scenario.NeverWorks)
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElse(sendToManualQueue("mrsdavis"))
      .orElseSucceed(
        "ERROR: User could not be saved, even to the fallback system"
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
      // TODO We are concerned about zipParLeft +
      // forkDaemon being introduced in the same
      // step
      .zipParLeft(
        userSignupInitiated("mrsdavis")
          .forkDaemon
      ).debug

// concurrently save & send analytics, ignoring analytics failures
object Eight extends ZIOAppDefault:
  override def run =
    // TODO Consider ways to dedup mrsdavis
    // string
    saveUser("mrsdavis")
      .timeoutFail(TimeoutError)(5.seconds)
      .retry(recurs(3) && spaced(1.second))
      .orElse(sendToManualQueue("mrsdavis"))
      .tapBoth(
        error =>
          userSignUpFailed("mrsdavis", error),
        success =>
          userSignupSucceeded(
            "mrsdavis",
            success
          )
      )
      .orElseSucceed(
        "ERROR: User could not be saved"
      )
      .debug
end Eight
