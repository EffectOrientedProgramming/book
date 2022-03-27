package bigdec

import zio.ZIO
import zio.test.Assertion.{
  diesWithA,
  equalTo,
  fails,
  failsWithA,
  isSubtype
}
import zio.test.{
  DefaultRunnableSpec,
  ErrorMessage,
  TestConsole,
  TestResult,
  assert,
  assertCompletes,
  assertTrue
}
import zio.test.TestAspect.silent

object MainSpec extends DefaultRunnableSpec:
  def spec =
    suite("MainSpec")(
      test("must succeed with valid value") {
        for
          _ <- TestConsole.feedLines("1")
          result <-
            inputBigDecimalValue("Num: ", 1, 10)
        yield assertTrue(result == BigDecimal(1))
      },
      test("must fail with non-parsable input") {
        for
          _ <- TestConsole.feedLines("a")
          error <-
            inputBigDecimalValue("Num: ", 1, 10)
              .mapError(_.getMessage)
              .exit
        yield assert(error)(
          fails(equalTo("Invalid input."))
        )
      },
      test("must fail with out-of-range input") {
        for
          _ <- TestConsole.feedLines("0")
          error <-
            inputBigDecimalValue("Num: ", 1, 10)
              .mapError(_.getMessage)
              .exit
        yield assert(error)(
          fails(
            equalTo(
              "Input out of the range from 1 to 10"
            )
          )
        )
      }
    ) @@ silent
end MainSpec
