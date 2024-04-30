package mdoc

import mdoc.internal.cli.InputFile
import mdoc.internal.markdown.MarkdownFile

import scala.meta.Input

object MainSpec extends ZIOSpecDefault:
  def spec =
    suite("mdoc.MainSpec"):
      test("mdoc run gets console output"):
        val mainSettings = MainSettings()

        val source =
          """```scala mdoc
            |class Foo extends mdoctools.ToRun:
            |  def run =
            |    Console.printLine("hello, world")
            |
            |Foo().runAndPrintOutput()
            |```
            |""".stripMargin

        val input =
          Input.VirtualFile(
            "foo.md",
            source
          )

        val inputFile =
          InputFile.fromRelativeFilename("foo.md", mainSettings.settings)

        val markdown =
          MarkdownFile
            .parse(input, inputFile, mainSettings.settings)

        val out =
          mdoc.processMarkdown(mainSettings.settings, mainSettings.reporter, markdown)

        assertTrue(out.renderToString.contains("// hello, world"))
      //@@ TestAspect.nonFlaky @@ TestAspect.repeats(100)
      +
      test("mdoc test gets console output"):
        val mainSettings = MainSettings()
          //.withArgs(List("--verbose"))

        val source =
          """```scala mdoc
            |class FooSpec extends mdoctools.ToTest:
            |  def spec =
            |    test("foo"):
            |      defer:
            |        ZIO.succeed("hello, debug").debug.run
            |        Console.printLine("hello, world").run
            |        assertCompletes
            |
            |FooSpec().runAndPrintOutput()
            |```
            |""".stripMargin

        val input =
          Input.VirtualFile(
            "foo.md",
            source
          )

        val inputFile =
          InputFile.fromRelativeFilename("foo.md", mainSettings.settings)

        val markdown =
          MarkdownFile
            .parse(input, inputFile, mainSettings.settings)

        val out =
          mdoc.processMarkdown(mainSettings.settings, mainSettings.reporter, markdown)

        val outString = out.renderToString

        assertTrue:
          outString.contains("// hello, debug") &&
          outString.contains("// hello, world") &&
          outString.contains("// \u001B[32m+\u001B[0m foo")
      +
      test("mdoc test with TestClock"):
        val mainSettings = MainSettings()
//          .withArgs(List("--verbose"))

        val source =
          """```scala mdoc
            |class FooSpec extends mdoctools.ToTest:
            |  def spec =
            |    test("foo"):
            |      defer:
            |        val fork = Console.printLine("hello, world").delay(24.hours).fork.run
            |        TestClock.adjust(24.hours).run
            |        fork.join.run
            |        assertCompletes
            |
            |FooSpec().runAndPrintOutput()
            |```
            |""".stripMargin

        val input =
          Input.VirtualFile(
            "foo.md",
            source
          )

        val inputFile =
          InputFile.fromRelativeFilename("foo.md", mainSettings.settings)

        val markdown =
          MarkdownFile
            .parse(input, inputFile, mainSettings.settings)

        val out =
          mdoc.processMarkdown(mainSettings.settings, mainSettings.reporter, markdown)

        val outString = out.renderToString

        assertTrue:
          outString.contains("// hello, world") &&
            outString.contains("// \u001B[32m+\u001B[0m foo")
