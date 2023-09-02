package mdoc.modifiers

import mdoc.{PostModifier, PostModifierContext}

import scala.jdk.CollectionConverters.*

class RunModifier extends PostModifier:
  override val name = "run"

  def process(ctx: PostModifierContext): String =

    val originalCodeFenceText =
      ctx.originalCode.text

    val updated =
      originalCodeFenceText
        .replace("runDemo(\n", "")
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
end RunModifier
