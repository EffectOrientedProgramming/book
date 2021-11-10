package testcontainers

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*

// TODO Figure out fi
// TESTCONTAINERS_RYUK_DISABLED=true is a
// band-aid that's avoiding the real problem with
// test cleanup

object TestContainersSpec
    extends DefaultRunnableSpec:

  import zio.durationInt

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test("With managed layer") {
        // TODO
        val logicWithAssertions =
          for
            people <- ContainerScenarios.logic
          yield assert(people.head)(
            equalTo(
              Person("Joe", "Dimagio", 143)
            )
          )
        val layer = ContainerScenarios.layer

        logicWithAssertions
          .provideSomeLayer[ZTestEnv & ZEnv](
            layer
          )
      },
      test("stream approach") {
        for
          res <- ZIO.succeed(1)
        yield assert(res)(equalTo(1))
      }
    )
end TestContainersSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
