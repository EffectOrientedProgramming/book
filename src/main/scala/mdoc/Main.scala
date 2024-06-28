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
import zio.test.*

// todo: get rid of mutables
def embed(
    codeFence: CodeFence,
    num: Int
): CodeFence =
  val sb =
    StringBuilder()
  codeFence.renderToString(sb)

  if codeFence
      .getMdocMode
      .exists(_.startsWith("runzio"))
  then
    val useLiveClockString =
      if codeFence
          .getMdocMode
          .contains("runzio:liveclock")
      then
        "(useLiveClock = true)"
      else
        ""
    val pre =
      s"class Chapter$num extends mdoctools.ToRun$useLiveClockString:"
    val post =
      s"Chapter$num().runAndPrintOutput()"
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
      s"class Chapter${num}Spec extends mdoctools.ToTest:"
    val post =
      s"Chapter${num}Spec().runAndPrintOutput()"
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
            line.contains("ToRun") ||
            line.contains("runAndPrintOutput()")
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
    val linesWithoutMdocGunk =
      codeFence
        .newPart
        .getOrElse(codeFence.body.value)
        .linesIterator
        .filterNot {
          line =>
            line.contains("ToTest:") ||
            line.contains("runAndPrintOutput()")
        }
        .map(_.stripPrefix("  "))
        .toSeq

    val outputLines =
      linesWithoutMdocGunk
        .reverse
        .takeWhile(_.startsWith("//"))
        .reverse

    val codeLines =
      linesWithoutMdocGunk
        .dropRight(outputLines.length)

    val truncatedOutputLines =
      if outputLines.length >= 20 then
        outputLines.take(6) ++ Seq("// ...") ++
          outputLines.takeRight(6)
      else
        outputLines

    val truncatedLines =
      codeLines ++ truncatedOutputLines

    val newBody =
      truncatedLines
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

case class RunnableCodeFence(
    codeFence: CodeFence,
    num: Int
)

def partsToExamples(
    markdownFile: MarkdownFile,
    baseName: String
): (String, String) =
  def runnableCodeFence(
      codeFence: CodeFence,
      mainCodeFences: Seq[
        CodeFence | RunnableCodeFence
      ]
  ): RunnableCodeFence =
    val rcfs =
      mainCodeFences.collect:
        case rcf: RunnableCodeFence =>
          rcf
    RunnableCodeFence(codeFence, rcfs.size)

  val (mainParts, testParts) =
    markdownFile
      .parts
      .foldLeft(
        (
          Seq.empty[
            CodeFence | RunnableCodeFence
          ],
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
                acc._1 :+
                  runnableCodeFence(
                    codeFence,
                    acc._1
                  ),
                acc._2
              )
            case codeFence: CodeFence
                if codeFence
                    .info
                    .value
                    .contains("testzio") &&
                  !codeFence
                    .info
                    .value
                    .contains(
                      "manuscript-only"
                    ) =>
              (acc._1, acc._2 :+ codeFence)
            case codeFence: CodeFence
                if codeFence
                  .info
                  .value
                  .contains("scala") &&
                  !codeFence
                    .info
                    .value
                    .contains("mdoc:crash") &&
                  !codeFence
                    .info
                    .value
                    .contains("mdoc:fail") &&
                  !codeFence
                    .info
                    .value
                    .contains(
                      "mdoc:compile-only"
                    ) &&
                  !codeFence
                    .info
                    .value
                    .contains(
                      "manuscript-only"
                    ) &&
                  !codeFence
                    .info
                    .value
                    .contains(
                      "testzio"
                    ) =>
              (acc._1 :+ codeFence, acc._2)
            case _ =>
              (acc._1, acc._2)
      }

  val runCode =
    if mainParts.isEmpty then
      ""
    else
      // todo: sandbox so otherParts across
      // chapters do not conflict
      mainParts
        .map:
          case rcf: RunnableCodeFence =>
            val body =
              rcf
                .codeFence
                .newBody
                .getOrElse(
                  rcf.codeFence.body.value
                )
            val indented =
              body
                .linesIterator
                .map("  " + _)
                .mkString("\n")
            s"""object App${rcf.num} extends helpers.ZIOAppDebug:
             |$indented
             |""".stripMargin

          case codeFence: CodeFence =>
            codeFence.body.value
        .mkString(
          s"""package Chapter$baseName
         |
         |import zio.*
         |import zio.direct.*
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
        .zipWithIndex
        .map {
          (part, num) =>
            if part.info.value.contains("mdoc:silent") then
              part.newBody.getOrElse(part.body.value)
            else
              val body =
                part
                  .newBody
                  .getOrElse(part.body.value)
              val indented =
                body
                  .linesIterator
                  .map("  " + _)
                  .mkString("\n")
              s"""object Test$num extends ZIOSpecDefault:
                 |$indented
                 |""".stripMargin
        }
        .mkString(
          s"""package Chapter$baseName
             |
             |import zio.*
             |import zio.direct.*
             |import zio.test.*
             |
             |""".stripMargin,
          "\n\n",
          ""
        )

  (runCode, testCode)
end partsToExamples

def processMarkdown(
    settings: Settings,
    reporter: Reporter,
    runnableMarkdown: MarkdownFile
) =
  val context =
    Context.fromSettings(settings, reporter)

  val processor =
    new Processor()(context.get)
  val processed =
    processor.processDocument(runnableMarkdown)
  processed

def manuscriptPost(
    markdownFile: MarkdownFile
): MarkdownFile =
  val parts =
    markdownFile
      .parts
      .flatMap:
        case cf: CodeFence
            if cf.newInfo.contains("scala\n") ||
              cf.info.value == "scala\n" =>
          val newBody =
            cf.newBody.getOrElse(cf.body.value)
          val (codeLines, outputLines) =
            newBody
              .linesIterator
              .toSeq
              .foldRight(
                (
                  Seq.empty[String],
                  Seq.empty[String]
                )
              ):
                (line, acc) =>
                  if acc._1.isEmpty &&
                    line.startsWith("//")
                  then
                    (
                      acc._1,
                      line
                        .stripPrefix("// ")
                        .stripPrefix("//") +:
                        acc._2
                    )
                  else
                    (line +: acc._1, acc._2)

          if outputLines.nonEmpty then
            List(
              CodeFence(
                Text("```"),
                Text("scala\n"),
                Text(codeLines.mkString("\n")),
                Text("\n```\n")
              ),
              Text("\nOutput:\n\n"),
              CodeFence(
                Text("```"),
                Text("shell\n"),
                Text(outputLines.mkString("\n")),
                Text("\n```\n")
              ) // mdoc doesn't seem to support ```text
            )
          else
            List(cf)
        case other =>
          List(other)

  markdownFile.copy(parts =
    parts
  )
end manuscriptPost

def processFile(
    input: Input,
    inputFile: InputFile,
    settings: Settings,
    reporter: Reporter
): (MarkdownFile, MarkdownFile) =
  val source =
    input
      .text
      .replace("```scala 3", "```scala")
      .replace("import zio.*\n\n", "")
      .replace("import zio.*\n", "")
      .replace("import zio.direct.*\n\n", "")
      .replace("import zio.direct.*\n", "")

  val preprocessedInput =
    Input.String(source)

  val parsed =
    MarkdownFile.parse(
      preprocessedInput,
      inputFile,
      settings
    )

  val runnableMarkdown =
    parsedToRunnable(parsed, settings)

  val processed: MarkdownFile =
    processMarkdown(
      settings,
      reporter,
      runnableMarkdown
    )

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

  val manuscriptMarkdown =
    runnableMarkdown.copy(parts =
      withoutRunnableParts.map {
        case codeFence: CodeFence
            if codeFence
              .info
              .value
              .contains("scala mdoc") =>
          // turn scala mdoc(*) into just scala
          codeFence.newInfo =
            Some(
              codeFence
                .info
                .value
                .takeWhile(_ != ' ') + "\n"
            )
          codeFence
        case p: MarkdownPart =>
          p
      }
    )

  manuscriptPost(manuscriptMarkdown) ->
    withoutRunnable
end processFile

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

  val (manuscriptMarkdown, withoutRunnable) =
    processFile(
      input,
      inputFile,
      newSettings,
      mainSettings.reporter
    )

  if mainSettings.reporter.hasErrors then
    println(
      s"Not writing outputs due to errors in ${inputFile.inputFile.toRelative}"
    )
    // todo: show just the error block?
  else
    // write manuscript
    Files.createDirectories(
      inputFile.outputFile.toNIO.getParent
    )
    Files.write(
      inputFile.outputFile.toNIO,
      ErrorMessageManipulation.cleanupZioErrorOutput(
        manuscriptMarkdown.renderToString
      ).getBytes(mainSettings.settings.charset)
    )

    val baseName =
      withoutRunnable
        .file
        .inputFile
        .toFile
        .getName
        .replace(".md", "")

    // todo: filter mdoc:fails
    val (mainParts, testParts) =
      partsToExamples(withoutRunnable, baseName)

    // todo: the code fences remain scala
    // mdoc:runzio and not sure if that is a
    // problem for leanpub
    val runOut =
      Option.when(mainParts.nonEmpty):
        val mainFile =
          examplesDir.resolve(
            s"src/main/scala/Chapter$baseName.scala"
          )
        Files.createDirectories(
          mainFile.toNIO.getParent
        )
        Files.write(
          mainFile.toNIO,
          mainParts.getBytes(
            mainSettings.settings.charset
          )
        )
        mainFile

    val testOut =
      Option.when(testParts.nonEmpty):
        val testFile =
          examplesDir.resolve(
            s"src/test/scala/Chapter${baseName}Spec.scala"
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
    println(s"""Processing:
        |  ${inputFile.inputFile.toRelative}
        |""".stripMargin)

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
  import scala.collection.parallel.ParSeq

  Files
    .list(mainSettings.settings.in.head.toNIO)
    .toScala(ParSeq)
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
end processDir


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
