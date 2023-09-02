package mdoc.modifiers

import mdoc.{PostModifier, PostModifierContext}
import scala.jdk.CollectionConverters.*

class SpecModifier extends PostModifier:
  override val name = "spec"
  def process(ctx: PostModifierContext): String =

    val originalCodeFenceText =
      ctx.originalCode.text

    val updated =
      originalCodeFenceText
        .replace("runSpec(\n", "")
        .lines()
        .toList
        .asScala
        .dropRight(1)
        .map(_.stripPrefix("  "))
        .mkString("\n")

    val newOutput =
      ctx
        .outputCode
        .replace(ctx.originalCode.text, updated)

    s"""```scala
      |$newOutput
      |```
      |""".stripMargin
  end process
end SpecModifier
