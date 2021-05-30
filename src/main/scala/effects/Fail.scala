//Fail.scala

package effects

import zio._
import java.io
import java.io.IOException

object Fail {

  @main def MainFail() =
    //fail(1-3) are effects that fail with the specified value
    //Any parameter type can be passes in including other ZIO.
    val fail1: IO[Int, Nothing] = ZIO.fail(12)
    val fail2: IO[String, Nothing] = ZIO.fail("Hello")

    val bar: foo2 = foo2()
    val fail3: IO[foo2, Nothing] = ZIO.fail(bar)

    val zioEx2: ZIO[zio.console.Console, IOException, Unit] =
      console.putStrLn("ZIO")

    def processWithSelfDescribedFallbackBehavior(
        success: Boolean
    ): ZIO[Any, ZIO[zio.console.Console, IOException, Unit], String] =
      if (success)
        ZIO.succeed("Good job!")
      else
        ZIO.fail(zioEx2)

  /*
    val fullProgram =
      for
        stringOrZio <- processWithSelfDescribedFallbackBehavior(true).catchAll {
          case x: ZIO[zio.console.Console, IOException, Unit] => x
        }
      yield ()

   */
  def getCreditScoreFromAgency1(
      successful: Boolean,
      fallbackIsSuccessful: Boolean
  ) =
    if (successful)
      ZIO.succeed(550)
    else if (fallbackIsSuccessful)
      ZIO.succeed(575)
    else ZIO.fail("Could not get their personal info")

  def getCreditScoreFromAgency2(successful: Boolean) =
    if (successful)
      ZIO.succeed(557)
    else ZIO.fail("Internal system failure")

  // Looking into where to provide fallback behavior
  val getCreditScore: ZIO[zio.console.Console, IOException, Int] =
    getCreditScoreFromAgency1(false, true).catchAll { case failureReason =>
      for
        _ <- console.putStrLn("First agency failure: " + failureReason)
        creditScore <- getCreditScoreFromAgency2(true).catchAll(_ =>
          ZIO.succeed(700)
        )
      yield (creditScore)
    }
}

case class foo2()
