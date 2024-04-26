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
            |    println("asdf")
            |    Console.printLine("hello, world")
            |Foo().getOrThrowFiberFailure()
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
      @@ TestAspect.nonFlaky @@ TestAspect.repeats(100)
