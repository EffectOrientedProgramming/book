// Fail.scala

package zioBasics

import zio._
import java.io
import java.io.IOException

object Fail:

  @main
  def MainFail() =
    // fail(1-3) are effects that fail with the
    // specified value
    // Any parameter type can be passed in.
    val fail1: IO[Int, Nothing] =
      ZIO.fail(12) //ZIO that fails with 12
    val fail2: IO[String, Nothing] =
      ZIO.fail(
        "Hello"
      ) //ZIO that fails with Hello

    val bar: foo2 = foo2()
    val fail3: IO[foo2, Nothing] =
      ZIO.fail(
        bar
      ) //ZIO that fails with an object

    val zioEx2
        : ZIO[Has[Console], IOException, Unit] =
      Console.printLine("ZIO")

    // ZIO can even fail with other ZIO. Here is
    // an example of where a function can define
    // it's own fallback behavior.
    // Althought there may be better ways of
    // defining such a function, it is valid to
    // return another ZIO on fail.
    def processWithSelfDescribedFallbackBehavior(
        success: Boolean
    ): ZIO[Any, ZIO[Has[
      Console
    ], IOException, Unit], String] =
      if (success)
        ZIO.succeed("Good job!")
      else
        ZIO.fail(zioEx2)
  end MainFail

  // Here is a more complex example of using ZIO
  // fails in the context of an app searching for
  // a person's credit score.

  def getCreditScoreFromAgency1( //This function checks to see if the credict score can be found from Agency 1
      successful: Boolean,
      fallbackIsSuccessful: Boolean
  ) =
    if (successful)
      ZIO.succeed(550)
    else if (fallbackIsSuccessful)
      ZIO.succeed(575)
    else
      ZIO.fail(
        "Could not get their personal info"
      )

  def getCreditScoreFromAgency2(
      successful: Boolean
  ) = //This function checks to see if the credict score can be found from Agency 2
    if (successful)
      ZIO.succeed(557)
    else
      ZIO.fail("Internal system failure")

  // Here we use the different ZIO-based
  // functions to string together a coherent
  // piece of logic.
  val getCreditScore
      : ZIO[Has[Console], IOException, Int] =
    getCreditScoreFromAgency1(false, true)
      .catchAll { case failureReason =>
        for
          _ <-
            Console.printLine(
              "First agency failure: " +
                failureReason
            )
          creditScore <-
            getCreditScoreFromAgency2(true)
              .catchAll(_ => ZIO.succeed(700))
        yield creditScore
      }
end Fail

case class foo2()
