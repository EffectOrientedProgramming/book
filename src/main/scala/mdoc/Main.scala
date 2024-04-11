package mdoc

import mdoc.internal.cli.{Context, InputFile}
import mdoc.internal.markdown.{MarkdownFile, Processor}
import mdoc.parser.MarkdownPart
import mdoc.parser.{CodeFence, Text}

import java.nio.file.Files
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import scala.{StringBuilder, main}
import scala.jdk.StreamConverters.*

def processFile(inputFile: InputFile, examplesDir: AbsolutePath, mainSettings: MainSettings): Unit =

  println(inputFile)
  println(examplesDir)

  val source =
    FileIO.slurp(
      inputFile.inputFile,
      mainSettings.settings.charset
    )

  //  println(source)

  val input =
    Input.VirtualFile(
      inputFile.inputFile.toString(),
      source
    )
  //  println(input)

  val parsed =
    MarkdownFile
      .parse(input, inputFile, mainSettings.settings)

  def embed(codeFence: CodeFence, line: String): CodeFence =
    val newBody = line +: codeFence.body.value.linesIterator.toSeq.map("  " + _)
    codeFence.newInfo = Some("scala mdoc")
    codeFence.newBody = Some(newBody.mkString("\n"))
    codeFence

  val newParts = parsed.parts.map {
    case codeFence: CodeFence if codeFence.getMdocMode.contains("runzio") =>
      embed(codeFence, "runDemo:")
    case codeFence: CodeFence if codeFence.getMdocMode.contains("testzio") =>
      embed(codeFence, "runSpec:")
    case codeFence: CodeFence =>
      codeFence
    case text: Text =>
      text
  }

  val newInputString = StringBuilder()
  newParts.foreach { part =>
    part.renderToString(newInputString)
  }
  val newInput = input.copy(value = newInputString.toString)
  //  println(s"newInput = $newInput")

  val newParsed = MarkdownFile.parse(newInput, inputFile, mainSettings.settings)
  //val parsedPre = parsed.copy(parts = newParts)//, input = newInput)
  //parsedPre.appendText("asdfzxcv")
  //println(parsedPre)

  val context = Context.fromSettings(mainSettings.settings, mainSettings.reporter)

  val processor = new Processor()(context.get)
  val processed = processor.processDocument(newParsed)

  def unembed(codeFence: CodeFence): CodeFence =
    codeFence.copy(
      body = codeFence.newBody.fold(Text("")) { body =>
        Text(body.linesIterator.drop(1).map(_.stripPrefix("  ")).mkString("\n"))
      }
    )

  val forManuscript = processed.parts.map {
    case codeFence: CodeFence if codeFence.body.value.startsWith("runDemo:") || codeFence.body.value.startsWith("runSpec:") =>
      unembed(codeFence)
    case codeFence: CodeFence =>
      codeFence
    case text: Text =>
      text
  }

  val manuscript = StringBuilder()
  forManuscript.foreach { part =>
    part.renderToString(manuscript)
  }

  // todo: we could maybe turn the escape coloring into spans
  val asciiManuscript = manuscript.toString.replaceAll("(\\u001B\\[\\d+m)", "") // remove the escape coloring

  Files.createDirectories(inputFile.outputFile.toNIO.getParent)
  Files.write(inputFile.outputFile.toNIO, asciiManuscript.getBytes(mainSettings.settings.charset))

  type RunDemo = CodeFence
  type RunSpec = CodeFence

  val (runDemoParts, runSpecParts, otherParts) = processed.parts.foldLeft((Seq.empty[RunDemo], Seq.empty[RunSpec], Seq.empty[MarkdownPart])) { (acc, part) =>
    part match
      case codeFence: CodeFence if codeFence.body.value.startsWith("runDemo:") =>
        (acc._1 :+ codeFence, acc._2, acc._3)
      case codeFence: CodeFence if codeFence.body.value.startsWith("runSpec:") =>
        (acc._1, acc._2 :+ codeFence, acc._3)
      case codeFence: CodeFence =>
        (acc._1, acc._2, acc._3 :+ part)
      case _ =>
        (acc._1, acc._2, acc._3)
  }

  val baseName = inputFile.inputFile.toFile.getName.replace(".md", "")

  val runDemosSeq: Seq[String] = runDemoParts.zipWithIndex.map { (codeFence, i) =>
    val lines = Seq(
      s"object ${baseName}_$i extends ZIOAppDefault:",
      s"  def run ="
    )
    val newBody = lines ++: unembed(codeFence).body.value.linesIterator.toSeq.map { line =>
      if !line.startsWith("//") then
        "    " + line
      else
        line
    }
    newBody.mkString("\n")
  }

  val runDemos = runDemosSeq.mkString("\n\n")
  val mainFile = examplesDir.resolve(s"src/main/scala/$baseName.scala")
  Files.createDirectories(mainFile.toNIO.getParent)
  Files.write(mainFile.toNIO, runDemos.getBytes(mainSettings.settings.charset))


  val other = StringBuilder()
  otherParts.foreach { part =>
    part.renderToString(other)
  }

  // todo: what to do with these?
  println(other)

  val runSpecs: String = runSpecParts.zipWithIndex.map { (codeFence, i) =>
    val lines = Seq(
      s"    test(\"test$i\"):"
    )
    val newBody = lines ++: unembed(codeFence).body.value.linesIterator.toSeq.map { line =>
      "      " + line
    }
    newBody.mkString("\n")
  }.mkString(
    s"""
      |object ${baseName}Spec extends ZIOSpecDefault:
      |  def spec = suite(\"suite\")(
      |""".stripMargin,
    """
      |    ,
      |""".stripMargin,
    """
      |  )
      |""".stripMargin
  ).replaceAll("(\\u001B\\[\\d+m)", "") // remove the escape coloring

  val testFile = examplesDir.resolve(s"src/test/scala/${baseName}Spec.scala")
  Files.createDirectories(testFile.toNIO.getParent)
  Files.write(testFile.toNIO, runSpecs.getBytes(mainSettings.settings.charset))

  ()


@main
def mdocRun(examplesDir: String) =
  val mainSettings =
    mdoc
      .MainSettings()
      .withArgs(List("--verbose"))

  Files.list(mainSettings.settings.in.head.toNIO).toScala(LazyList).foreach { file =>
    val inputFile =
      InputFile.fromRelativeFilename(
        file.toFile.getName,
        mainSettings.settings
      )
    println(inputFile)
    processFile(inputFile, AbsolutePath(examplesDir), mainSettings)
  }

