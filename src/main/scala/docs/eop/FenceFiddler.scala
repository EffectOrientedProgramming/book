package docs.eop

import mdoc._

import java.nio.file.{Files, Path, Paths}
import scala.meta.inputs.Position
import scala.language.unsafeNulls

class FenceFiddler extends PostModifier {
  val name = "fiddler"

  def process(
      ctx: PostModifierContext
  ): String = {
    val relpath: Path = Paths.get(ctx.info)
    ctx.lastValue match {
//      case d: Drawable =>
// Files.createDirectories(out.getParent)
// if (!Files.isRegularFile(out)) {
//          d.write(out.toFile)
//        }
//        s"![](${ctx.info})"
      case _ =>
        val (pos, obtained) =
          ctx.variables.lastOption match {
            case Some(variable) =>
              val prettyObtained =
                s"${variable.staticType} = ${variable.runtimeValue}"
              (variable.pos, prettyObtained)
            case None =>
              (
                Position.Range(
                  ctx.originalCode,
                  0,
                  0
                ),
                "nothing"
              )
          }
        s"Pos: $pos"
//        ctx.reporter.error(
//          pos,
//          s"""type mismatch:
// expected:
        // com.cibo.evilplot.geometry.Drawable
//  obtained: $obtained"""
//        )

        import org.scalafmt.interfaces.Scalafmt

        import java.nio.file._
        val scalafmt = Scalafmt.create(
          this.getClass.getClassLoader
        )
        val config =
          Paths.get(".scalafmt.conf")
        import collection.JavaConverters._
        val read = Files
          .readAllLines(config)
          .asScala
          .mkString("\n")
        println(read)
        val file = Paths.get("Main.scala")

        val wrappedCode =
          s"""
             |object Fenced {
             |${ctx.outputCode}
             |}
             |""".stripMargin

        val formattedOutput =
          scalafmt.format(
            config,
            file,
            wrappedCode
          )

        println(formattedOutput)

        val fencedAndFormatted =
          s"""
            |```scala
            |$formattedOutput
            |```
            |""".stripMargin

        import java.nio.file.Files
//        Files.write(
//          ctx.outputFile.toNIO,
//          fencedAndFormatted.getBytes
//        )
        fencedAndFormatted
    }
  }
}
