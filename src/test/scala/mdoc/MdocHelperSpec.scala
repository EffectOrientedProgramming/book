package mdoc

import zio.*
import zio.Console.*
import zio.test.*
import zio.test.Assertion.*

import java.io.IOException

object MdocHelperSpec extends ZIOSpecDefault:

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test(
        "Intercept and format MatchError from unhandled RuntimeException"
      ) {
        for output <-
            ZIO.succeed(
              unsafeRunPrettyPrintValue(
                ZIO.succeed(
                  throw new MatchError(
                    MdocSession
                      .App
                      .GpsException()
                  )
                )
              )
            )
        yield assertTrue(
          output == "Defect: GpsException"
        )
      },
      test(
        "Ensure successful result lines are all below length limit"
      )(
        for _ <-
            ZIO
              .attempt(
                unsafeRunPrettyPrint(
                  ZIO.succeed("A" * 50)
                )
              )
              .flip
        yield assertCompletes
      ),
      test(
        "Concisely renders a custom Exception"
      ) {
        for result <-
            ZIO
              .attempt(
                unsafeRunPrettyPrint(
                  ZIO.attempt(
                    throw MdocSession
                      .App
                      .SuperDeeplyNested
                      .NameThatShouldBreakRendering
                      .CustomException()
                  )
                )
              )
              .debug
        yield assertCompletes
      },
      test("Handle HelloFailures situation") {
        val badMsg =
          """
            |error: repl.MdocSession$MdocApp$GpsException
            |        at repl.MdocSession.MdocApp.<local MdocApp>.getTemperatureZWithFallback(14_Hello_Failures.md:250)
            |        at mdoc.MdocHelpers$package.unsafeRunPrettyPrint(MdocHelpers.scala:78)
            |""".stripMargin
        for result <-
            ZIO
              .attempt(
                unsafeRunPrettyPrint(
                  ZIO.succeed(badMsg)
                )
              )
              .flip
        yield assertCompletes
      },
      test("Invoke failure with stack trace") {
        for result <-
            ZIO
              .attempt(
                unsafeRunPrettyPrint(
                  ZIO.attempt(foo())
                )
              )
              .flip // TODO Better assertions around line lengths
              .debug
        yield assertCompletes
      }
    )
end MdocHelperSpec

object MdocSession:
  object App:
    object SuperDeeplyNested:
      object NameThatShouldBreakRendering:
        class CustomException()
            extends Exception()

    case class GpsException()
        extends RuntimeException
