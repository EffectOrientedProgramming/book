package mdoc

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeEvent.EventType
import mdoc.internal.cli.{
  Context,
  InputFile,
  Settings
}
import mdoc.internal.io.MdocFileListener
import mdoc.internal.livereload.UndertowLiveReload
import mdoc.internal.markdown.{
  MarkdownFile,
  Processor
}
import mdoc.parser.{
  CodeFence,
  MarkdownPart,
  Text
}

import java.nio.file.{Files, Paths}
import java.util.concurrent.Executors
import scala.jdk.StreamConverters.*
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath

// todo: get rid of mutables
def embed(
    codeFence: CodeFence,
    num: Int
): CodeFence =
  val sb =
    StringBuilder()
  codeFence.renderToString(sb)

  if codeFence.getMdocMode.contains("runzio")
  then
    val pre =
      s"class Example$num extends mdoctools.ToRun:"
    val post =
      s"Example$num().getOrThrowFiberFailure()"
    val newBody =
      pre +:
        codeFence
          .body
          .value
          .linesIterator
          .toSeq
          .map("  " + _) :+ post
//    codeFence.newInfo = Some("scala mdoc:runzio")
//    codeFence.newBody = Some(newBody.mkString("\n"))
    codeFence.copy(
      info =
        Text("scala mdoc:runzio\n"),
      body =
        Text(newBody.mkString("\n"))
    )
  else if codeFence
      .getMdocMode
      .contains("testzio")
  then
    val pre =
      s"val spec$num = mdoctools.ToTest:"

    val post =
      s"spec$num.getOrThrowFiberFailure()"
    val newBody =
      pre +:
        codeFence
          .body
          .value
          .linesIterator
          .toSeq
          .map("  " + _) :+ post
    codeFence.newInfo =
      Some("scala mdoc:testzio")
    codeFence.newBody =
      Some(newBody.mkString("\n"))
    codeFence
  else
    codeFence
  end if
end embed

// todo: get rid of mutables
def unembed(codeFence: CodeFence): CodeFence =
  if codeFence.getMdocMode.contains("runzio")
  then
    val newBody =
      codeFence
        .newPart
        .getOrElse(codeFence.body.value)
        .linesIterator
        .filterNot {
          line =>
            line.contains("ToRun:") ||
            line.contains(
              "getOrThrowFiberFailure()"
            )
        }
        .map(_.stripPrefix("  "))
        .mkString("\n")

    codeFence.newBody =
      Some(newBody)
    codeFence.newPart =
      None
    codeFence.newInfo =
      Some("scala mdoc:runzio")
    codeFence
  else if codeFence
      .getMdocMode
      .contains("testzio")
  then
    val newBody =
      codeFence
        .newPart
        .getOrElse(codeFence.body.value)
        .linesIterator
        .filterNot {
          line =>
            line.contains("ToTest:") ||
            line.contains(
              "getOrThrowFiberFailure()"
            )
        }
        .map(_.stripPrefix("  "))
        .mkString("\n")
        .replaceAll(
          "(\\u001B\\[\\d+m)",
          ""
        ) // remove the escape coloring

    codeFence.newBody =
      Some(newBody)
    codeFence.newPart =
      None
    codeFence.newInfo =
      Some("scala mdoc:testzio")
    codeFence
  else
    codeFence

def parsedToRunnable(
    markdownFile: MarkdownFile,
    settings: Settings
): MarkdownFile =
  val runnableParts =
    markdownFile
      .parts
      .zipWithIndex
      .map {
        case (codeFence: CodeFence, num) =>
          embed(codeFence, num)
        case (m: MarkdownPart, _) =>
          m
      }

  val runnableMarkdownFile =
    markdownFile.copy(parts =
      runnableParts
    )

  val newInputString =
    runnableMarkdownFile.renderToString
  val newInput =
    Input.String(newInputString)

  // todo: reparsing loses the original code but
  // maybe we can avoid the reparse
  MarkdownFile
    .parse(newInput, markdownFile.file, settings)
end parsedToRunnable

def partsToExamples(
    markdownFile: MarkdownFile,
    baseName: String
): (String, String) =
  val (runParts, testParts, otherParts) =
    markdownFile
      .parts
      .foldLeft(
        (
          Seq.empty[CodeFence],
          Seq.empty[CodeFence],
          Seq.empty[CodeFence]
        )
      ) {
        (acc, part) =>
          part match
            case codeFence: CodeFence
                if codeFence
                  .getMdocMode
                  .contains("runzio") =>
              (
                acc._1 :+ codeFence,
                acc._2,
                acc._3
              )
            case codeFence: CodeFence
                if codeFence
                  .getMdocMode
                  .contains("testzio") =>
              (
                acc._1,
                acc._2 :+ codeFence,
                acc._3
              )
            case codeFence: CodeFence
                if codeFence
                  .info
                  .value
                  .contains("scala") &&
                  !codeFence
                    .info
                    .value
                    .contains("mdoc:fail") &&
                  !codeFence
                    .info
                    .value
                    .contains(
                      "mdoc:compile-only"
                    ) =>
              (
                acc._1,
                acc._2,
                acc._3 :+ codeFence
              )
            case _ =>
              (acc._1, acc._2, acc._3)
      }

  // todo: this approach loses the ordering of
  // the original
  val runCode =
    if runParts.isEmpty then
      ""
    else
      // todo: sandbox so otherParts across
      // chapters do not conflict
      val otherPartsCode =
        otherParts
          .map(_.body.value)
          .mkString("\n\n")

      runParts
        .zipWithIndex
        .map {
          (part, num) =>
            val body =
              part
                .newBody
                .getOrElse(part.body.value)
            val indented =
              body
                .linesIterator
                .map("  " + _)
                .mkString("\n")
            s"""object Example${baseName}_$num extends ZIOAppDefault:
           |$indented
           |""".stripMargin
        }
        .mkString(
          s"""import zio.*
         |import zio.direct.*
         |
         |$otherPartsCode
         |
         |""".stripMargin,
          "\n\n",
          ""
        )

  val testCode =
    if testParts.isEmpty then
      ""
    else
      testParts
        .map {
          part =>
            part
              .newBody
              .getOrElse(part.body.value)
              .linesIterator
              .map("    " + _)
              .mkString("\n")
        }
        .mkString(
          s"""import zio.*
         |import zio.direct.*
         |import zio.test.*
         |
         |object Example${baseName}Spec extends ZIOSpecDefault:
         |  def spec = suite(\"suite\"):
         |""".stripMargin,
          "\n    + ",
          "\n"
        ).replace("+     ", "+ ") // hackier

  (runCode, testCode)
end partsToExamples

def processFile(
    inputFile: InputFile,
    examplesDir: AbsolutePath,
    mainSettings: MainSettings
): Unit |
  (
      AbsolutePath,
      Option[AbsolutePath],
      Option[AbsolutePath]
  ) =
  mainSettings.reporter.reset()

  val newSettings =
    mainSettings
      .settings
      .copy(
        scalacOptions =
          mainSettings
            .settings
            .scalacOptions
            .replace("-unchecked", "")
            .replace("-deprecation", ""),
        postModifiers =
          List(
            RunZIOPostModifier(),
            TestZIOPostModifier()
          ) // we use the PostModifiers so we can keep passing the "scala mdoc:runzio" and "scala mdoc:testzio" through
      )

  val source =
    FileIO.slurp(
      inputFile.inputFile,
      newSettings.charset
    )

  val input =
    Input.VirtualFile(
      inputFile.inputFile.toString(),
      source
    )

  val parsed =
    MarkdownFile
      .parse(input, inputFile, newSettings)

  val runnableMarkdown =
    parsedToRunnable(parsed, newSettings)
  // println(runnableMarkdown.renderToString)

  val context =
    Context.fromSettings(
      newSettings,
      mainSettings.reporter
    )

  val processor =
    new Processor()(context.get)
  val processed =
    processor.processDocument(runnableMarkdown)

  val withoutRunnableParts =
    processed
      .parts
      .map {
        case codeFence: CodeFence =>
          unembed(codeFence)
        case m: MarkdownPart =>
          m
      }

  val withoutRunnable =
    runnableMarkdown.copy(parts =
      withoutRunnableParts
    )

  if mainSettings.reporter.hasErrors then
    println("Not writing outputs due to errors")
    // todo: show just the error block?
    // println(runnableMarkdown.renderToString)
  else
    // write manuscript
    Files.createDirectories(
      inputFile.outputFile.toNIO.getParent
    )
    Files.write(
      inputFile.outputFile.toNIO,
      withoutRunnable
        .renderToString
        .getBytes(mainSettings.settings.charset)
    )

    val baseName =
      withoutRunnable
        .file
        .inputFile
        .toFile
        .getName
        .replace(".md", "")

    // todo: filter mdoc:fails
    val (runParts, testParts) =
      partsToExamples(withoutRunnable, baseName)

    // todo: the code fences remain scala
    // mdoc:runzio and not sure if that is a
    // problem for leanpub
    val runOut =
      Option.when(runParts.nonEmpty):
        val mainFile =
          examplesDir.resolve(
            s"src/main/scala/Example$baseName.scala"
          )
        Files.createDirectories(
          mainFile.toNIO.getParent
        )
        Files.write(
          mainFile.toNIO,
          runParts.getBytes(
            mainSettings.settings.charset
          )
        )
        mainFile

    val testOut =
      Option.when(testParts.nonEmpty):
        val testFile =
          examplesDir.resolve(
            s"src/test/scala/Example${baseName}Spec.scala"
          )
        Files.createDirectories(
          testFile.toNIO.getParent
        )
        Files.write(
          testFile.toNIO,
          testParts.getBytes(
            mainSettings.settings.charset
          )
        )
        testFile

    (inputFile.outputFile, runOut, testOut)
  end if
end processFile

def fileChange(
    event: DirectoryChangeEvent,
    mainSettings: MainSettings,
    examplesDir: String,
    force: Boolean
): Option[AbsolutePath] =
  val inputFile =
    InputFile.fromRelativeFilename(
      event.path().toFile.getName,
      mainSettings.settings
    )

  val needsUpdate =
    force ||
      !inputFile.outputFile.toFile.exists() ||
      inputFile.inputFile.toFile.lastModified() >
      inputFile.outputFile.toFile.lastModified()
  if event.path().toString.endsWith(".md") &&
    needsUpdate
  then
    println("Processing:")
    println(
      "  " + inputFile.inputFile.toRelative
    )

    processFile(
      inputFile,
      AbsolutePath(examplesDir),
      mainSettings
    ) match
      case (
            manuscriptFile,
            maybeMainFile,
            maybeTestFile
          ) =>
        println("Done:")
        println("  " + manuscriptFile.toRelative)
        maybeMainFile.foreach {
          mainFile =>
            println("  " + mainFile.toRelative)
        }
        maybeTestFile.foreach {
          testFile =>
            println("  " + testFile.toRelative)
        }
        println()
      case _ =>
        ()
    end match
    // we use the lastModified to determine if
    // the manuscript is up-to-date
    event
      .path()
      .toFile
      .setLastModified(
        inputFile
          .outputFile
          .toFile
          .lastModified()
      )
    Some(inputFile.outputFile)
  else
    None
  end if
end fileChange

def processDir(
    examplesDir: String,
    mainSettings: MainSettings,
    force: Boolean =
      false
): Unit =
  Files
    .list(mainSettings.settings.in.head.toNIO)
    .toScala(LazyList)
    .foreach {
      file =>
        val directoryChangeEvent =
          DirectoryChangeEvent(
            EventType.MODIFY,
            false,
            file,
            null,
            1,
            mainSettings.settings.in.head.toNIO
          )
        fileChange(
          directoryChangeEvent,
          mainSettings,
          examplesDir,
          force
        )
    }

@main
def mdocRun(examplesDir: String) =
  val mainSettings =
    mdoc.MainSettings()
    // .withArgs(List("--verbose"))

  processDir(examplesDir, mainSettings)

  // with "scala mdoc:fail"
  // for some reason the program deadlocks on
  // exit, so we force it
  java.lang.System.exit(0)

// todo: probably a better way for flags like --verbose and --force
@main
def mdocRunForce(
    examplesDir: String,
    file: String
) =
  val mainSettings =
    mdoc.MainSettings()
    // .withArgs(List("--verbose"))

  val directoryChangeEvent =
    DirectoryChangeEvent(
      EventType.MODIFY,
      false,
      Paths.get(file),
      null,
      1,
      mainSettings.settings.in.head.toNIO
    )

  fileChange(
    directoryChangeEvent,
    mainSettings,
    examplesDir,
    true
  )

  // with "scala mdoc:fail"
  // for some reason the program deadlocks on
  // exit, so we force it
  java.lang.System.exit(0)
end mdocRunForce

// todo: probably a better way for flags like --verbose and --force
@main
def mdocRunForceAll(examplesDir: String) =
  val mainSettings =
    mdoc.MainSettings()
    // .withArgs(List("--verbose"))

  processDir(
    examplesDir,
    mainSettings,
    force =
      true
  )

  // with "scala mdoc:fail"
  // for some reason the program deadlocks on
  // exit, so we force it
  java.lang.System.exit(0)

@main
def mdocWatch(examplesDir: String) =
  val mainSettings =
    mdoc.MainSettings()
    // .withArgs(List("--verbose"))

  val livereload =
    UndertowLiveReload(
      mainSettings.settings.out.head.toNIO,
      reporter =
        mainSettings.reporter
    )
  livereload.start()

  // process the markdowns before setting up the
  // watcher
  processDir(examplesDir, mainSettings)

  val executor =
    Executors.newFixedThreadPool(1)
  val watcher =
    MdocFileListener.create(
      mainSettings.settings.in,
      executor,
      java.lang.System.in
    ) {
      directoryChangeEvent =>
        val maybeChanged =
          fileChange(
            directoryChangeEvent,
            mainSettings,
            examplesDir,
            false
          )
        maybeChanged.foreach {
          absolutePath =>
            livereload.reload(absolutePath.toNIO)
        }
    }
  println("press any key to stop the watcher")
  watcher.watchUntilInterrupted()
  livereload.stop()
end mdocWatch
