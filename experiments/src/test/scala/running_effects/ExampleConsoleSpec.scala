package running_effects

import zio.test.*

object ExampleConsoleSpec extends ZIOSpecDefault:
  val promptForUsername = ZIO.succeed("Zeb")
  def spec =
    test("console IO"):
      defer:
        val username = promptForUsername.run
        Console.printLine(username).run
        Console.printLine("X-X").run
        Console.printLine("-X-").run
        Console.printLine("X-X").run
        val capturedOutput: Vector[String] = TestConsole.output.run
        val expectedOutput =
          s"""|Zeb
              |X-X
              |-X-
              |X-X
              |""".stripMargin
        assertTrue(
          capturedOutput.mkString("") == expectedOutput

        )
