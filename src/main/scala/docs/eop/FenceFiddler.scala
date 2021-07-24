package docs.eop

import mdoc._

import java.nio.file.{Files, Path, Paths}
import scala.meta.inputs.Position
import scala.language.unsafeNulls
import org.scalafmt.interfaces.Scalafmt
import java.nio.file._
import collection.JavaConverters._
import com.typesafe.config.ConfigFactory

// TODO Figure out how to support other premade
// modifiers while using our custom modifier
class FenceFiddler extends PostModifier:
  val name = "fiddler"

  def process(ctx: PostModifierContext): String =
    val relpath: Path = Paths.get(ctx.info)

    val scalafmt =
      Scalafmt
        .create(this.getClass.getClassLoader)
    val config = Paths.get(".scalafmt.conf")

    val file = Paths.get("Main.scala")

    val conf =
      ConfigFactory.parseFile(config.toFile)

    val numberOfLinesBeforeEndMarkerIsInserted =
      conf.getInt(
        "rewrite.scala3.insertEndMarkerMinLines"
      )

    val wrappedCode =
      s"""object Fenced {
         |${ctx.outputCode}
         |}
         |""".stripMargin

    val formattedOutput: String =
      scalafmt.format(config, file, wrappedCode)

    def dropLeadingIndentionIfPresent(
        input: String
    ) =
      if (input.take(2) == "  ")
        input.drop(2)
      else
        input

    val outputtedLines =
      formattedOutput.split("\n").toList

    val formattedWithoutObjectWrapper =
      (
        if (
          outputtedLines.length >=
            numberOfLinesBeforeEndMarkerIsInserted
        ) then
          outputtedLines.dropRight(1)
        else
          outputtedLines
      ).drop(1)
        .map(dropLeadingIndentionIfPresent)
        .mkString("\n")

    val fencedAndFormatted =
      s"""
         |```scala
         |$formattedWithoutObjectWrapper
         |```
         |""".stripMargin

    fencedAndFormatted
  end process
end FenceFiddler
