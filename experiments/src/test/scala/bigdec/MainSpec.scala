package bigdec

// TODO Determine if there is a clear reason to include these tests
import zio.test.Assertion.*
import zio.test.*
import zio.test.TestAspect.silent

object MainSpec extends ZIOSpecDefault:
  def spec =
    suite("MainSpec")(
      test("must succeed with valid value") {
        defer {
          TestConsole.feedLines("1").run
          assertTrue(
            inputBigDecimalValue("Num: ", 1, 10)
              .run == BigDecimal(1)
          )
        }
      },
      test("must fail with non-parsable input") {
        defer {
          TestConsole.feedLines("a").run
          val error =
            inputBigDecimalValue("Num: ", 1, 10)
              .mapError(_.getMessage)
              .exit
              .run
          assert(error)(
            fails(equalTo("Invalid input."))
          )
        }
      },
      test("must fail with out-of-range input") {
        defer {
          TestConsole.feedLines("0").run
          val error =
            inputBigDecimalValue("Num: ", 1, 10)
              .mapError(_.getMessage)
              .exit
              .run
          assert(error)(
            fails(
              equalTo(
                "Input out of the range from 1 to 10"
              )
            )
          )
        }
      }
    ) @@ silent
end MainSpec
