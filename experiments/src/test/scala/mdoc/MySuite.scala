package mdoc

import zio.*
import zio.Console.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*

import java.io.IOException

object MdocHelperSpec
    extends DefaultRunnableSpec:

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test(
        "Intercept and format MatchError from unhandled RuntimeException"
      ) {
        for
          _ <-
            mdoc.wrapUnsafeZIO(
              ZIO.succeed(
                throw new MatchError(
                  MdocSession.App.GpsException()
                )
              )
            )
          output <- TestConsole.output
        yield assert(output)(
          equalTo(
            Vector(
              "Defect: class scala.MatchError\n",
              "        GpsException\n"
            )
          )
        )
      }
    )
end MdocHelperSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
