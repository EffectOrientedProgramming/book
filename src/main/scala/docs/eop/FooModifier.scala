package docs.eop

import mdoc.{Reporter, StringModifier}

import scala.meta.Input

class FooModifier extends StringModifier {
  override val name = "foo"

  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    val originalCodeFenceText = code.text
    val isCrash = info == "crash"
    if (isCrash) "BOOM"
    else "OK: " + originalCodeFenceText

    import org.scalafmt.interfaces.Scalafmt

    import java.nio.file._
    val scalafmt = Scalafmt.create(
      this.getClass.getClassLoader
    )
    val config = Paths.get(".scalafmt.conf")
    import collection.JavaConverters._
    import scala.language.unsafeNulls

    val read = Files
      .readAllLines(config)
      .nn
      .asScala
      .nn
      .mkString("\n")
    println(read)
    val file = Paths.get("Main.scala")

    val wrappedCode =
      s"""
        |object Fenced {
        |$originalCodeFenceText
        |}
        |""".stripMargin

    scalafmt.format(
      config,
      file,
      wrappedCode
    )
  }
}
