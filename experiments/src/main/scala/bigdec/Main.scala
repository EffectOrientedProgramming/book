package bigdec

import zio.{ZIO, ZIOAppDefault, Console, Schedule}

def inputBigDecimalValue(prompt: String, min: BigDecimal, max: BigDecimal): ZIO[Console, Exception, Unit] =
  for
    _ <- Console.printLine(prompt)
    s <- Console.readLine
    n <- ZIO.attempt(BigDecimal(s)).mapError(_ => Exception("Invalid input."))
    _ <- ZIO.unless(min <= n && n <= max)(ZIO.fail(Exception(s"Input out of the range from $min to $max")))
  yield ()


object Main extends ZIOAppDefault:
  def run = inputBigDecimalValue("Enter a number", 1, 10)
    .tapError(e => Console.printLineError(e.getMessage))
    .retry(Schedule.forever)
