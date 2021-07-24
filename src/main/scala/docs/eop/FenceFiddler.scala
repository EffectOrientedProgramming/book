package docs.eop

import mdoc._

import java.nio.file.{Files, Path, Paths}
import scala.meta.inputs.Position
import scala.language.unsafeNulls

// TODO Figure out how to support other premade
// modifiers while using our custom modifier
class FenceFiddler extends PostModifier:
  val name = "fiddler"

  def process(ctx: PostModifierContext): String =
    val relpath: Path = Paths.get(ctx.info)
    val (pos, obtained) =
      ctx.variables.lastOption match
        case Some(variable) =>
          val prettyObtained =
            s"${variable.staticType} = ${variable.runtimeValue}"
          (variable.pos, prettyObtained)
        case None =>
          (
            Position
              .Range(ctx.originalCode, 0, 0),
            "nothing"
          )
    s"Pos: $pos"

    import org.scalafmt.interfaces.Scalafmt

    import java.nio.file._
    val scalafmt =
      Scalafmt
        .create(this.getClass.getClassLoader)
    val config = Paths.get(".scalafmt.conf")
    import collection.JavaConverters._
    val file = Paths.get("Main.scala")

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

    val formattedWithoutObjectWrapper =
      formattedOutput
        .split("\n")
        .toList
        .drop(1)
        .dropRight(1)
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
