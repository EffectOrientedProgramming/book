package bigdec

import zio.{
  ZIO,
  ZIOAppDefault,
  Console,
  Schedule
}
import zio.direct.*

def inputBigDecimalValue(
    prompt: String,
    min: BigDecimal,
    max: BigDecimal
): ZIO[Any, Exception, BigDecimal] =
  defer {
    Console.printLine(prompt).run
    val input = Console.readLine.run
    val result =
      ZIO
        .attempt(BigDecimal(input))
        .mapError(_ =>
          Exception("Invalid input.")
        ).run
    ZIO.unless(min <= result && result <= max)(
      ZIO.fail(
        Exception(
          s"Input out of the range from $min to $max"
        )
      )
    ).run
    result
  }

object Main extends ZIOAppDefault:
  def run =
    inputBigDecimalValue("Enter a number", 1, 10)
      .tapError(e =>
        Console.printLineError(e.getMessage)
      )
      .retry(Schedule.forever)
