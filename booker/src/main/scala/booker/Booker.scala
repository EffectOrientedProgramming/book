package booker

import tui.{TUI, TerminalApp, TerminalEvent}
import view._
import zio.{Scope, Unsafe, ZEnvironment, ZIO, ZIOAppArgs, ZIOAppDefault}
import zio.Console._
import zio.Runtime.unsafe

import java.io.{File, FileNotFoundException}
import scala.io.Source
import scala.util.Try

object BookerTools {
  def withLeadingZero(i: Int) =
    if (i > 9)
      i.toString
    else
      s"0$i"

  // interestingly these are effects but are not
  // wrapped in ZIO
  def validateDir(
                   dir: File
                 ): ZIO[Any, FileNotFoundException, Unit] = {
    ZIO
      .fail(
        new FileNotFoundException(
          s"$dir was not found or was not a directory"
        )
      )
      .unless(dir.exists() && dir.isDirectory)
      .map(_ => ())
  }

  def filesInDir(dir: File): Seq[File] =
    dir.listFiles().toSeq

  def parseChapter(f: File): Option[(Int, File)] = {

    def intPrefix(s: String): Option[(Int, File)] =
      Try(Integer.parseInt(s)).toOption.map(_ -> f)

    if (f.getName.endsWith(".md"))
      f.getName
        .split('_')
        .headOption
        .flatMap(intPrefix)
    else
      None
  }

  def chapterFiles(dir: File): Seq[(Int, File)] = {

    val files = filesInDir(dir)
    files.flatMap(parseChapter)
  }
    // def duplicates(
    //    files: Seq[(Int, File)]
    // ): Seq[(Int, File)] =
    //  val justNums = files.toSeq.map(_._1)
    //  val dups =
    //    justNums.diff(justNums.distinct).distinct
    //  files.filter { f =>
    //    dups.contains(f._1)
    //  }

  def resolveDups(
                   dups: Seq[File]
                 ): ZIO[Any, Throwable, Seq[File]] = {

  val something =
    for {
      _ <- printLine("Conflict detected:")
      _ <- ZIO.foreach(dups.zipWithIndex) {
          case (f, i) =>
            printLine(s"$i) ${f.getName}")
        }
      _ <- printLine("\nWhich one should be first:")
      numString <- readLine
      num <- ZIO.fromTry(
          Try(Integer.parseInt(numString))
        ) // todo: retry if unparsable
      (firstIndex, restIndex) = dups.zipWithIndex.partition(_._2 == num)
      firstSeq = firstIndex.map(_._1)
      rest = restIndex.map(_._1)
      first <-
        if (firstSeq.size == 1)
          ZIO.succeed(firstSeq.head)
        else
          ZIO.fail(
            new Exception("Invalid index")
          ) // todo: retry
      resolution <-
        if (rest.size > 1)
          resolveDups(rest)
        else
          ZIO.succeed(rest)
    } yield first +: resolution
  something
}

  /* File:
 * 01-a 02-foo 02-bar 03-fiz
 *
 * GroupedFiles:
 * Seq(01-a), Seq(02-foo, 02-bar), Seq(03-fiz)
 * Seq(04-foo, 04-bar),
 *
 * Dups:
 * 02-foo 02-bar
 *
 * Resolutions:
 * 02-foo 03-bar */

  def filesWithChapterIndexes(dir: File):
    ZIO[Any, Throwable, Seq[(Int, Seq[File])]] = {
    for {
      _ <- validateDir(dir)
      _ <- printLine(s"Reordering $dir")
    }
    yield
      chapterFiles(dir)
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2))
        .toSeq
        .sortBy(_._1)
  }

  def program(
               dir: File
             ): ZIO[Any, Throwable, Unit] = {
    for {
      grouped <- filesWithChapterIndexes(dir)
      results <-
        ZIO.foreach(grouped)(dups =>
          if (dups._2.length > 1)
            resolveDups(dups._2)
          else
            ZIO.succeed(dups._2)
        )
      flatResults = results.flatten
      // Now, strip out numbers and rename
      // according to place in this sequence
      _
        <-
        ZIO.attempt {
          flatResults
            .zipWithIndex
            .map { case (file, index) =>
              rename(file, index)
            }
        }
      _
        <-
        printLine(
          s"Completed with ${flatResults.size} files"
        )
      _
        <-
        printLine(
          s"Potential Re-ordering: \n" +
            flatResults.mkString("\n")
        )
    }
    yield ()
  }

  def rename(original: File, index: Int) =
    original
      .renameTo(renameRep(original, index))

  def renameRep(original: File, index: Int) = {

    val stripped =
      original.getName.dropWhile(_ != '_').drop(1)

    def cleanupName(s: String): String = {
      s.stripPrefix("# ")
        .replace(' ', '_')
        .replaceAll("[^0-9a-zA-Z_]", "") + ".md"
    }

    val source = Source.fromFile(original)

    val fromMarkdown =
      source
        .getLines()
        .nextOption()
        .map(cleanupName)
        .getOrElse(stripped)

    source.close()

    val withLeadingZero =
      if (index > 9)
        index.toString
      else
        s"0$index"

    val name = withLeadingZero + "_" + fromMarkdown

    new File(original.getParentFile, name)
  }

  def withMarkdownNames(
                           files: Seq[File]
                         ): ZIO[Any, FileNotFoundException, Seq[File]] = {
      println(files)
      ZIO.succeed(files)
  }

}

case class CliState(files: Seq[(Int, Seq[File])], cursorIdx: Int = 0) {
  val (conflicting, nonConflicting) =
    files
      .partition(_._2.size > 1)
//      .flatMap(tuple => tuple._2)

  val conflicts =
    conflicting
      .flatMap(tuple => tuple._2)

}

case class CliStateSimp(files: Seq[File], cursorIdx: Int = 0, newFileName: String = "") {
  val fileNameRep = {
    val name =
      if (newFileName.isEmpty)
        "???"
      else
        newFileName.capitalize

    BookerTools.withLeadingZero(cursorIdx) + "_" + name + ".md"
  }


}

/*
Conflict detected:
0) 01_Introduction.md
1) 01_asdf.md

Which one should be first:


Conflict detected. Choose the first one:
[ ] 01_Intro.md
[x] 01_Asdf.md

 */

object BookerApp extends TerminalApp[Nothing, CliState, String] {
  override def render(state: CliState): View = {
    val indexedView =
      state.nonConflicting.map(f => (f._1, View.text(f._2.head.toString())))

    val conflict =
      state.conflicting.head // TODO Make safe.

    val conflictView: Seq[(Int, View)] =
      conflict._2
        .zipWithIndex
        .map { case (file, idx) =>
          if (idx == state.cursorIdx)
            View.text("▣" + file.toString).green
          else
            View.text("☐" + file.toString).cyan.dim
        }.map( (conflict._1, _))

    val sortedViews =
      (indexedView ++ conflictView).sortBy(_._1)

    View.vertical(
      View.vertical(
        sortedViews.map(_._2): _*
      ),
    )
  }

  override def update(
                       state: CliState,
                       event: TerminalEvent[Nothing]
                     ): TerminalApp.Step[CliState, String] = {
    event match {
      case TerminalEvent.UserEvent(_) =>
        ???
      case TerminalEvent.SystemEvent(keyEvent) =>
        keyEvent match {
          case KeyEvent.Up =>
            if (state.cursorIdx == 0)
              TerminalApp.Step.update(state)
            else
              TerminalApp.Step.update(state.copy(cursorIdx = state.cursorIdx - 1))
          case KeyEvent.Down =>
            if (state.cursorIdx == 1)
              TerminalApp.Step.update(state)
            else
              TerminalApp.Step.update(state.copy(cursorIdx = state.cursorIdx + 1))
          case KeyEvent.Enter =>
//            val firstFile: File = state.conflicts(state.cursorIdx)
            val flatFiles =
              state.files
                .flatMap {
                  case (_, filesWithIdx) =>
                    if (filesWithIdx.length == 1)
                      filesWithIdx
                    else {
                      val (firstFileWithIndex, secondFileWithIndex) =
                        filesWithIdx.zipWithIndex.partition(_._2 == state.cursorIdx)
                      val firstFile: File = firstFileWithIndex.head._1
                      val secondFile: File = secondFileWithIndex.head._1
                      Seq(firstFile, secondFile)
                    }
                }
            flatFiles
              .zipWithIndex
              .map { case (file, index) =>
                BookerTools.rename(file, index)
              }

            TerminalApp.Step.succeed(s"Reordered files!")


          case KeyEvent.Escape | KeyEvent.Exit | KeyEvent.Character('q') =>
            TerminalApp.Step.exit
          case _ =>
            TerminalApp.Step.update(state)
        }
    }
  }
}

object BookerReorderApp extends TerminalApp[Nothing, CliStateSimp, String] {
  override def render(state: CliStateSimp): View = {
    View.vertical(
    state.files.zipWithIndex
      .flatMap { case (file, index) =>
        val newFileGroup =
          if (index == state.cursorIdx) {
            Seq(
              View.text("New Chapter: " + state.fileNameRep).green,
            )
          } else {
            Seq()
          }

        val existingFileGroup =
          if (index >=  state.cursorIdx) {
            Seq(
            View.text(BookerTools.renameRep(file, index + 1).toString)
            )

          } else
            Seq(
              View.text(file.toString)
            )

        newFileGroup ++ existingFileGroup
      }: _*
    )
  }

  override def update(
                       state: CliStateSimp,
                       event: TerminalEvent[Nothing]
                     ): TerminalApp.Step[CliStateSimp, String] = {
    event match {
      case TerminalEvent.UserEvent(_) =>
        ???
      case TerminalEvent.SystemEvent(keyEvent) =>
        keyEvent match {
          case c :KeyEvent.Character =>
              TerminalApp.Step.update(state.copy(newFileName = state.newFileName + c.char))
          case KeyEvent.Delete =>
            if (state.newFileName.nonEmpty)
              TerminalApp.Step.update(state.copy(newFileName = state.newFileName.init))
            else
              TerminalApp.Step.update(state)
          case KeyEvent.Up =>
            if (state.cursorIdx == 0)
              TerminalApp.Step.update(state)
            else
              TerminalApp.Step.update(state.copy(cursorIdx = state.cursorIdx - 1))
          case KeyEvent.Down =>
            if (state.cursorIdx < state.files.length - 1)
              TerminalApp.Step.update(state.copy(cursorIdx = state.cursorIdx + 1))
            else
              TerminalApp.Step.update(state)
          case KeyEvent.Enter =>
            new File("Chapters/" + state.fileNameRep).createNewFile()
            state.files
              .zipWithIndex
              .drop(state.cursorIdx)
              .foreach{ case (file, idx) => BookerTools.rename(file, idx + 1)}
            throw new NotImplementedError("Created a new file and renamed everything after it: ")


          case KeyEvent.Escape | KeyEvent.Exit =>
            TerminalApp.Step.exit
          case _ =>
            TerminalApp.Step.update(state)
        }
    }
  }
}

object Booker extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val f: File = new File("Chapters")

    for {
      files <- BookerTools.filesWithChapterIndexes(f)
      flatFiles = files
        .flatMap(_._2)
      result <- BookerReorderApp
            .run(CliStateSimp(flatFiles))
            .provide(TUI.live(false))
      _ <- printLine(result)
    } yield ()
  }
}

object BookerOld extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val f: File =
    //    File(args.headOption.getOrElse(""))
      new File("Chapters")

    BookerTools.program(f.getAbsoluteFile)
  }
}