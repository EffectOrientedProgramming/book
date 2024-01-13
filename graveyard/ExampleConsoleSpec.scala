package running_effects

import zio.test.*

object FeedLinesDemo extends ZIOSpecDefault:
  def spec =
    test("feedSomeLines"):
      defer:
        TestConsole.feedLines("Zep").run
        assertCompletes

object ExampleConsoleSpec extends ZIOSpecDefault:
  def notificationFor(username: String) =
    ZIO.succeed("Meeting @ 9")

  def spec =
    test("console IO"):
      defer:
        TestConsole.feedLines("Zeb").run
        val username =
          Console
            .readLine("Enter your name\n")
            .run
        Console.printLine(s"Hello $username").run
        val notification =
          notificationFor(username).run
        Console.printLine(notification).run
        val capturedOutput: Vector[String] =
          TestConsole.output.run
        val expectedOutput =
          s"""|Enter your name
              |Hello Zeb
              |Meeting @ 9
              |""".stripMargin
        assertTrue(
          capturedOutput.mkString("") ==
            expectedOutput
        )
end ExampleConsoleSpec
