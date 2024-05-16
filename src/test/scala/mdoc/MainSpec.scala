package mdoc

import mdoc.internal.cli.InputFile
import mdoc.internal.markdown.MarkdownFile

import java.nio.file.Files
import scala.meta.Input
import scala.meta.io.AbsolutePath

object MainSpec extends ZIOSpecDefault:
  def spec =
    suite("mdoc.MainSpec"):
      test("mdoc run gets console output"):
        val mainSettings =
          MainSettings()

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
          Input.VirtualFile("foo.md", source)

        val inputFile =
          InputFile.fromRelativeFilename(
            "foo.md",
            mainSettings.settings
          )

        val markdown =
          MarkdownFile.parse(
            input,
            inputFile,
            mainSettings.settings
          )

        val out =
          mdoc.processMarkdown(
            mainSettings.settings,
            mainSettings.reporter,
            markdown
          )

        assertTrue(
          out
            .renderToString
            .contains("// hello, world")
        )
      // @@ TestAspect.nonFlaky @@
      // TestAspect.repeats(100)
      +
        test("mdoc test gets console output"):
          val mainSettings =
            MainSettings()
          // .withArgs(List("--verbose"))

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
            Input.VirtualFile("foo.md", source)

          val inputFile =
            InputFile.fromRelativeFilename(
              "foo.md",
              mainSettings.settings
            )

          val markdown =
            MarkdownFile.parse(
              input,
              inputFile,
              mainSettings.settings
            )

          val out =
            mdoc.processMarkdown(
              mainSettings.settings,
              mainSettings.reporter,
              markdown
            )

          val outString =
            out.renderToString

          assertTrue:
            outString
              .contains("// hello, debug") &&
            outString
              .contains("// hello, world") &&
            outString.contains(
              "// \u001B[32m+\u001B[0m foo"
            )
        +
        test("mdoc:runzio:liveclock"):
          val mainSettings =
            MainSettings()

          val newSettings =
            mainSettings
              .settings
              .copy(postModifiers =
                List(
                  RunZIOPostModifier(),
                  TestZIOPostModifier()
                )
              )

          val source =
            """```scala mdoc:runzio:liveclock
          |def run = ZIO.sleep(1.second).timed
          |```
          |""".stripMargin

          val input =
            Input.VirtualFile("foo.md", source)

          val inputFile =
            InputFile.fromRelativeFilename(
              "foo.md",
              mainSettings.settings
            )

          val (manuscriptMarkdown, _) =
            processFile(
              input,
              inputFile,
              newSettings,
              mainSettings.reporter
            )

          val rendered =
            manuscriptMarkdown.renderToString
          assertTrue:
            rendered.contains("// Result: (PT1")
        +
        test("mdoc test with TestClock"):
          val mainSettings =
            MainSettings()
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
            Input.VirtualFile("foo.md", source)

          val inputFile =
            InputFile.fromRelativeFilename(
              "foo.md",
              mainSettings.settings
            )

          val markdown =
            MarkdownFile.parse(
              input,
              inputFile,
              mainSettings.settings
            )

          val out =
            mdoc.processMarkdown(
              mainSettings.settings,
              mainSettings.reporter,
              markdown
            )

          val outString =
            out.renderToString

          assertTrue:
            outString
              .contains("// hello, world") &&
            outString.contains(
              "// \u001B[32m+\u001B[0m foo"
            )
        +
        test("ordering"):
          val newIn =
            Files.createTempDirectory("chapters")

          val mainSettings =
            MainSettings().withIn(newIn)

          val newSettings =
            mainSettings
              .settings
              .copy(postModifiers =
                List(
                  RunZIOPostModifier(),
                  TestZIOPostModifier()
                )
              )

          val source =
            """```scala mdoc:invisible
            |val foo = "foo"
            |```
            |
            |```scala mdoc:runzio
            |def run = ZIO.debug(foo)
            |```
            |
            |```scala mdoc:silent
            |val asdf = "asdf"
            |```
            |
            |```scala mdoc:runzio
            |def run = ZIO.debug(asdf)
            |```
            |""".stripMargin

          Files.write(
            newIn.resolve("foo.md"),
            source.getBytes
          )

          val inputFile =
            InputFile.fromRelativeFilename(
              "foo.md",
              mainSettings.settings
            )

          val examplesDir =
            Files.createTempDirectory("examples")

          val examplesPath =
            AbsolutePath(examplesDir)

          processFile(
            inputFile,
            examplesPath,
            mainSettings
          )

          import scala.jdk.CollectionConverters.*

          val exampleSrc =
            Files
              .readAllLines(
                examplesDir.resolve(
                  "src/main/scala/Chapterfoo.scala"
                )
              )
              .asScala

          assertTrue(
            exampleSrc.indexWhere(
              _.contains("val foo")
            ) <
              exampleSrc.indexWhere(
                _.contains("Chapterfoo_0")
              ) &&
              exampleSrc.indexWhere(
                _.contains("Chapterfoo_0")
              ) <
              exampleSrc.indexWhere(
                _.contains("val asdf")
              ) &&
              exampleSrc.indexWhere(
                _.contains("val asdf")
              ) <
              exampleSrc.indexWhere(
                _.contains("Chapterfoo_1")
              )
          )
    @@ TestAspect.sequential
end MainSpec
