package experiments

import zio.test.*

object ConsoleOutputSpec
    extends ZIOSpecDefault:
  def spec =
    test("Capture output"):
      defer:
        printLine("Morty").run
        val out1 = TestConsole.output.run
        printLine("Beth").run
        val out2 = TestConsole.output.run
        printLine(s"$out1\n****\n$out2").run
        printLine(out2(1)).run
        assertCompletes

object NewerTestConsole
  extends ZIOSpecDefault:
  val spec =
    test("Substitute input"):
      defer:
        TestConsole.feedLines("Morty", "Beth").run
        val input = readLine.run
        printLine(input).run
        assertTrue(input == "Morty")
