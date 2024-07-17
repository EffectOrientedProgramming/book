package experiments

import zio.test.*

object ConsoleSpec extends ZIOSpecDefault:
  def spec =
    suite("Hijacking the Console")(
      test("Capturing output"):
        defer:
          printLine("output").run
          assertTrue(
            TestConsole.output.run ==
              Vector("output\n")
          )
      ,
      test("Substituting input"):
        defer:
          val console =
            ZIO.service[Console].run
          val input = console.readLine.run
          printLine(s"$input").run
          assertTrue(input == "input")
        .provideLayer(
          TestConsole.make(
            TestConsole.Data(
              List("input", "more"),
              Vector.empty,
            )
          )
        ),
    )
end ConsoleSpec
