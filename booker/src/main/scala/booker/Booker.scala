package booker

import tui.{TUI, TerminalApp, TerminalEvent}
import tui.view.*
import zio.{Scope, Unsafe, ZEnvironment, ZIO, ZIOAppArgs, ZIOAppDefault}
import zio.Console.*
import zio.Runtime.unsafe

import java.io.{File, FileNotFoundException, IOException}
import scala.io.Source
import scala.util.Try

object BookerTools:
  def orderedChapters(dir: File): ZIO[Any, IOException, Seq[File]] =
    for
      _ <- validateDir(dir)
      _ <- printLine(s"Reordering $dir")
    yield
      chapterFiles(dir)
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2))
        .toSeq
        .sortBy(_._1)
        .flatMap(_._2)

  def withLeadingZero(i: Int): String =
    if (i > 9)
      i.toString
    else
      s"0$i"

  private def validateDir(
                   dir: File
                 ): ZIO[Any, FileNotFoundException, Unit] =
    ZIO
      .fail(
        new FileNotFoundException(
          s"$dir was not found or was not a directory"
        )
      )
      .unless(dir.exists() && dir.isDirectory)
      .map(_ => ())

  def filesInDir(dir: File): Seq[File] =
    dir.listFiles().toSeq

  private def parseChapter(f: File): Option[(Int, File)] =
    def intPrefix(s: String): Option[(Int, File)] =
      Try(Integer.parseInt(s)).toOption.map(_ -> f)

    if (f.getName.endsWith(".md"))
      f.getName
        .split('_')
        .headOption
        .flatMap(intPrefix)
    else
      None

  private def chapterFiles(dir: File): Seq[(Int, File)] =
    val files = filesInDir(dir)
    files.flatMap(parseChapter)

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

  def scrollBetween(begin: Int, end: Int, state: CliStateSimp, event: KeyEvent.Up.type | KeyEvent.Down.type) =
    event match
      case KeyEvent.Up =>
        if (state.cursorIdx == begin)
          TerminalApp.Step.update(state)
        else
          TerminalApp.Step.update(state.copy(cursorIdx = state.cursorIdx - 1))
      case KeyEvent.Down =>
        if (state.cursorIdx < end - 1)
          TerminalApp.Step.update(state.copy(cursorIdx = state.cursorIdx + 1))
        else
          TerminalApp.Step.update(state)

case class CliStateSimp(files: Seq[File], cursorIdx: Int = 0, newFileName: String = ""):
  val fileNameRep =
    val name =
      if (newFileName.isEmpty)
        "???"
      else
        newFileName.capitalize

    BookerTools.withLeadingZero(cursorIdx) + "_" + name + ".md"

object ReorderExistingApp extends TerminalApp[Nothing, CliStateSimp, String]:
  override def render(state: CliStateSimp): View =
    View.vertical(
      state.files.zipWithIndex.map((file, idx) =>
        if (idx == state.cursorIdx)
          View.text(file.toString).green.bold.bordered
        else
          View.text(file.toString)
      ): _*
    )

  override def update(
                       state: CliStateSimp,
                       event: TerminalEvent[Nothing]
                     ): TerminalApp.Step[CliStateSimp, String] =
    event match
      case TerminalEvent.UserEvent(_) =>
        ???
      case TerminalEvent.SystemEvent(keyEvent) =>
        keyEvent match
          case KeyEvent.Escape | KeyEvent.Exit =>
            TerminalApp.Step.exit
          case k @ (KeyEvent.Up | KeyEvent.Down) =>
            BookerTools.scrollBetween(0, state.files.length, state, k)
          case _ =>
            TerminalApp.Step.update(state)

object AddNewChapterApp extends TerminalApp[Nothing, CliStateSimp, String]:
  override def render(state: CliStateSimp): View =
    View.vertical(
      state.files.zipWithIndex
        .flatMap { case (file, index) =>
          val newFileGroup =
            if (index == state.cursorIdx)
              Seq(
                View.text("New Chapter: " + state.fileNameRep).green,
              )
            else
              Seq()

          val existingFileGroup =
            if (index >=  state.cursorIdx)
              Seq( View.text(BookerTools.renameRep(file, index + 1).toString) )
            else
              Seq( View.text(file.toString) )

          newFileGroup ++ existingFileGroup
        }: _*
    )

  override def update(
                       state: CliStateSimp,
                       event: TerminalEvent[Nothing]
                     ): TerminalApp.Step[CliStateSimp, String] =
    event match
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
          case k @ (KeyEvent.Up | KeyEvent.Down) =>
              BookerTools.scrollBetween(0, state.files.length, state, k)
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

object Booker extends ZIOAppDefault:
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    val f: File = new File("Chapters")

    for
      flatFiles <- BookerTools.orderedChapters(f)
      result <-
        ReorderExistingApp
//        AddNewChapterApp
          .run(CliStateSimp(flatFiles))
          .provide(TUI.live(false))
      _ <- printLine(result)
    yield ()
