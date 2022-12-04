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
              unsafeRunPrettyPrint(
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
      }
    )
end MdocHelperSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
