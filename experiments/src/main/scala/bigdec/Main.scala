package bigdec

import zio.{
  ZIO,
  ZIOAppDefault,
  Console,
  Schedule
}

def inputBigDecimalValue(
    prompt: String,
    min: BigDecimal,
    max: BigDecimal
): ZIO[Any, Exception, BigDecimal] =
  for
    _     <- Console.printLine(prompt)
    input <- Console.readLine
    result <-
      ZIO
        .attempt(BigDecimal(input))
        .mapError(_ =>
          Exception("Invalid input.")
        )
    _ <-
      ZIO.unless(min <= result && result <= max)(
        ZIO.fail(
          Exception(
            s"Input out of the range from $min to $max"
          )
        )
      )
  yield result

object Main extends ZIOAppDefault:
  def run =
    inputBigDecimalValue("Enter a number", 1, 10)
      .tapError(e =>
        Console.printLineError(e.getMessage)
      )
      .retry(Schedule.forever)
