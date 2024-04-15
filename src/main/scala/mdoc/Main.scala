package mdoc

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeEvent.EventType
import mdoc.internal.cli.{Context, InputFile}
import mdoc.internal.io.MdocFileListener
import mdoc.internal.livereload.UndertowLiveReload
import mdoc.internal.markdown.{MarkdownFile, Processor}
import mdoc.parser.MarkdownPart
import mdoc.parser.{CodeFence, Text}

import java.nio.file.{Files, Path}
import java.util.concurrent.Executors
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import scala.jdk.StreamConverters.*
//import scala.jdk.CollectionConverters.*

def processFile(inputFile: InputFile, examplesDir: AbsolutePath, mainSettings: MainSettings): Unit =

//  println(inputFile)
//  println(examplesDir)

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

  // todo: there doesn't seem to be a way to maintain the origin info (ie "scala mdoc:runzio")
  //   so later in rendering we have to look in the body for runDemo, runSpec
  def embed(codeFence: CodeFence, pre: String, post: String): CodeFence =
    val newBody = pre +: codeFence.body.value.linesIterator.toSeq.map("  " + _) :+ post
    codeFence.newInfo = Some("scala mdoc")
    codeFence.newBody = Some(newBody.mkString("\n"))
    codeFence

  val newParts = parsed.parts.zipWithIndex.map {
    case (codeFence: CodeFence, i: Int) if codeFence.getMdocMode.contains("runzio") =>
      embed(
        codeFence,
        s"class Example$i extends ToRun:",
        s"Example$i().getOrThrowFiberFailure()"
      )

    case (m: MarkdownPart, _) =>
      m
    /*
    case codeFence: CodeFence if codeFence.getMdocMode.contains("testzio") =>
      embed(codeFence, "runSpec:")
    case codeFence: CodeFence =>
      codeFence
    case text: Text =>
      text

     */
  }

  val newInputString: StringBuilder = StringBuilder()
  newInputString.append(
    """```scala mdoc:invisible
      |trait ToRun:
      |  val bootstrap: ZLayer[Any, Nothing, Any] = ZLayer.empty
      |  def run: ZIO[Any, Exception, Unit]
      |
      |  def getOrThrowFiberFailure(): Unit =
      |    Unsafe.unsafe { implicit unsafe =>
      |      Runtime.unsafe.fromLayer(bootstrap).unsafe.run(run).getOrThrowFiberFailure()
      |    }
      |```
      |""".stripMargin
  )
  newParts.foreach { part =>
    part.renderToString(newInputString)
  }

  val newInput = input.copy(value = newInputString.toString)
  //println(s"newInput = $newInput")

  val newParsed = MarkdownFile.parse(newInput, inputFile, mainSettings.settings)
  //val parsedPre = parsed.copy(parts = newParts)//, input = newInput)
  //parsedPre.appendText("asdfzxcv")
  //println(parsedPre)

//  val newParsedParts = newParsed.parts.map {
//    case codeFence: CodeFence if codeFence.getMdocMode.contains("runzio") || codeFence.getMdocMode.contains("testzio") =>
//      val newCodeFence = codeFence.copy(info = Text("scala mdoc"))
//      newCodeFence.newInfo = Some(codeFence.info.value)
//      newCodeFence
//    case other =>
//      other
//  }
//  val newParsedWithUpdatedInfo = newParsed.copy(parts = newParsedParts)
//
//  newParsedWithUpdatedInfo.parts.foreach {
//    case part: CodeFence =>
//      println(part.newInfo)
//    case _ =>
//      ()
//  }

  val context = Context.fromSettings(mainSettings.settings, mainSettings.reporter)

  val processor = new Processor()(context.get)
  val processed = processor.processDocument(newParsed)

//  processed.parts.foreach {
//    case part: CodeFence =>
//      println(part.newInfo)
//    case _ =>
//      ()
//  }

//  def unembed(codeFence: CodeFence): CodeFence =
//    codeFence.copy(
//      body = codeFence.newBody.fold(Text("")) { body =>
//        Text(body.linesIterator.drop(1).map(_.stripPrefix("  ")).mkString("\n"))
//      }
//    )

  val forManuscript = processed.parts.map {
    //case codeFence: CodeFence if codeFence.newInfo.contains("scala mdoc:runDemo") || codeFence.body.value.startsWith("runSpec:") =>
    case codeFence: CodeFence if codeFence.newBody.exists(_.contains("ToRun:")) =>
      codeFence.copy(
        body = codeFence.newBody.fold(Text("")) { body =>
          val lines = body.linesIterator.filterNot { line =>
            line.contains("ToRun:") || line.contains("getOrThrowFiberFailure()")
          }.map(_.stripPrefix("  ")).mkString("\n")
          Text(lines)
        }
      )
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
//
//  type RunDemo = CodeFence
//  type RunSpec = CodeFence
//


  /*
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

   */


  // drop the ToRun
  val forExamples = processed.parts.drop(1).map {
    case codeFence: CodeFence if codeFence.newBody.exists(_.contains("ToRun:")) =>
      codeFence.newBody.fold("") { body =>
        body.replace("class Example", "object Example").replace("ToRun", "ZIOAppDefault").linesIterator.filterNot { line =>
          line.contains("getOrThrowFiberFailure")
        }.mkString("\n")
      }
    case codeFence: CodeFence =>
      codeFence.newBody.getOrElse(codeFence.body.value)
    case text: Text =>
      ""
  }.filterNot(_.isEmpty).mkString(
    """import zio.*
      |import zio.direct.*
      |
      |""".stripMargin,
    "\n\n",
    ""
  )

  val baseName = inputFile.inputFile.toFile.getName.replace(".md", "")

  /*
  val runDemosSeq: Seq[String] = runDemoParts.zipWithIndex.map { (codeFence, i) =>
    val lines = Seq(
      s"object Example${baseName}_$i extends ZIOAppDefault:",
      //s"  def run ="
    )
    val newBody = lines ++: unembed(codeFence).body.value.linesIterator.toSeq.map { line =>
      if !line.startsWith("//") then
        "    " + line
      else
        line
    }
    newBody.mkString("\n")
  }

  val runDemos = runDemosSeq.mkString(
    """
      |import zio.*
      |import zio.direct.*
      |
      |""".stripMargin,
    "\n\n",
    ""
  )

   */
  //println(forExamples)

  val mainFile = examplesDir.resolve(s"src/main/scala/Example$baseName.scala")
  Files.createDirectories(mainFile.toNIO.getParent)
  Files.write(mainFile.toNIO, forExamples.getBytes(mainSettings.settings.charset))

//
//  val other = StringBuilder()
//  otherParts.foreach { part =>
//    part.renderToString(other)
//  }
//
//  // todo: what to do with these?
//  //println(other)
//
//  val runSpecs: String = runSpecParts.zipWithIndex.map { (codeFence, i) =>
//    val lines = Seq(
//      s"    test(\"test$i\"):"
//    )
//    val newBody = lines ++: unembed(codeFence).body.value.linesIterator.toSeq.map { line =>
//      "      " + line
//    }
//    newBody.mkString("\n")
//  }.mkString(
//    s"""
//      |import zio.test.*
//      |
//      |object Example${baseName}Spec extends ZIOSpecDefault:
//      |  def spec = suite(\"suite\")(
//      |""".stripMargin,
//    """
//      |    ,
//      |""".stripMargin,
//    """
//      |  )
//      |""".stripMargin
//  ).replaceAll("(\\u001B\\[\\d+m)", "") // remove the escape coloring
//
//  if runSpecParts.nonEmpty then {
//    val testFile = examplesDir.resolve(s"src/test/scala/Example${baseName}Spec.scala")
//    Files.createDirectories(testFile.toNIO.getParent)
//    Files.write(testFile.toNIO, runSpecs.getBytes(mainSettings.settings.charset))
//    ()
//  }

  ()

def fileChange(event: DirectoryChangeEvent, mainSettings: MainSettings, examplesDir: String, force: Boolean): Option[AbsolutePath] = {
  val inputFile =
    InputFile.fromRelativeFilename(
      event.path().toFile.getName,
      mainSettings.settings
    )

  val needsUpdate = force || inputFile.inputFile.toFile.lastModified() > inputFile.outputFile.toFile.lastModified()
  if event.path().toString.endsWith(".md") && needsUpdate then {
    println(s"Processing ${event.path()}")

    processFile(inputFile, AbsolutePath(examplesDir), mainSettings)

    // we use the lastModified to determine if the manuscript is up-to-date
    event.path().toFile.setLastModified(inputFile.outputFile.toFile.lastModified())
    Some(inputFile.outputFile)
  }
  else {
    None
  }
}

def processDir(examplesDir: String, mainSettings: MainSettings, force: Boolean = false): Unit = {
  Files.list(mainSettings.settings.in.head.toNIO).toScala(LazyList).foreach { file =>
    val directoryChangeEvent = DirectoryChangeEvent(EventType.MODIFY, false, file, null, 1, mainSettings.settings.in.head.toNIO)
    fileChange(directoryChangeEvent, mainSettings, examplesDir, force)
  }
}

@main
def mdocRun(examplesDir: String) =
  val mainSettings =
    mdoc
      .MainSettings()
      .withArgs(List("--verbose"))

  processDir(examplesDir, mainSettings)

// todo: probably a better way for flags like --verbose and --force
@main
def mdocRunForce(examplesDir: String) =
  val mainSettings =
    mdoc
      .MainSettings()
      //.withArgs(List("--verbose"))

  processDir(examplesDir, mainSettings, force = true)

@main
def mdocWatch(examplesDir: String) =
  val mainSettings =
    mdoc
      .MainSettings()
      //.withArgs(List("--verbose"))

  processDir(examplesDir, mainSettings)

  val livereload = UndertowLiveReload(
    mainSettings.settings.out.head.toNIO,
    reporter = mainSettings.reporter,
  )
  livereload.start()

  val executor = Executors.newFixedThreadPool(1)
  val watcher = MdocFileListener.create(mainSettings.settings.in, executor, java.lang.System.in) { directoryChangeEvent =>
    val maybeChanged = fileChange(directoryChangeEvent, mainSettings, examplesDir, false)
    maybeChanged.foreach { absolutePath =>
      livereload.reload(absolutePath.toNIO)
    }
  }
  println("press any key to stop the watcher")
  watcher.watchUntilInterrupted()
  livereload.stop()
