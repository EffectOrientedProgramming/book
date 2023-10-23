package zio_intro

import zio.Schedule.*

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

  extension [R, E, A](z: ZIO[R, E, A])
    def fireAndForget(
        background: ZIO[R, Nothing, Any]
    ): ZIO[R, E, A] =
      z.zipParLeft(background.forkDaemon)

end HiddenPrelude
