package booker

import zio.{Console, Has, ZIO}
import zio.Console.*
import zio.Runtime.default.unsafeRun

import java.io.{
  File,
  FileNotFoundException,
  IOException
}
import scala.io.Source
import scala.util.CommandLineParser.FromString
import scala.util.Try

// interestingly these are effects but are not
// wrapped in ZIO
def validateDir(
    dir: File
): ZIO[Any, FileNotFoundException, Unit] =
  ZIO
    .fail(
      FileNotFoundException(
        s"$dir was not found or was not a directory"
      )
    )
    .unless(dir.exists() && dir.isDirectory)
    .map(_ => ())

def filesInDir(dir: File): Seq[File] =
  dir.listFiles().nn.map(_.nn).toSeq

def parseChapter(f: File): Option[(Int, File)] =
  def intPrefix(s: String): Option[(Int, File)] =
    Try(Integer.parseInt(s)).toOption.map(_ -> f)

  if f.getName.nn.endsWith(".md") then
    f.getName
      .nn
      .split('_')
      .nn
      .headOption
      .flatMap(intPrefix)
  else
    None

def chapterFiles(dir: File): Seq[(Int, File)] =
  val files = filesInDir(dir)
  files.flatMap(parseChapter)

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
): ZIO[Has[Console], Throwable, Seq[File]] =
  val something =
    for
      _ <- printLine("Conflict detected:")
      _ <-
        ZIO.foreach(dups.zipWithIndex) {
          case (f, i) =>
            printLine(s"$i) ${f.getName}")
        }
      _ <-
        printLine("\nWhich one should be first:")
      numString <- readLine
      num <-
        ZIO.fromTry(
          Try(Integer.parseInt(numString))
        ) // todo: retry if unparsable
      (firstIndex, restIndex) =
        dups.zipWithIndex.partition(_._2 == num)
      firstSeq = firstIndex.map(_._1)
      rest     = restIndex.map(_._1)
      first <-
        if firstSeq.size == 1 then
          ZIO.succeed(firstSeq.head)
        else
          ZIO.fail(
            new Exception("Invalid index")
          ) // todo: retry
      resolution <-
        if rest.size > 1 then
          resolveDups(rest)
        else
          ZIO.succeed(rest)
    yield first +: resolution

  something
end resolveDups

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

def program(dir: File) =
  for
    _ <- validateDir(dir)
    _ <- printLine(s"Reordering $dir")
    files = chapterFiles(dir)
    grouped: Seq[(Int, Seq[File])] =
      files
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2))
        .toSeq
        .sortBy(_._1)
    results: Seq[Seq[File]] <-
      ZIO.foreach(grouped)(dups =>
        if (dups._2.length > 1)
          resolveDups(dups._2)
        else
          ZIO.succeed(dups._2)
      )
    flatResults = results.flatten
    // Now, strip out numbers and rename
    // according to place in this sequence
    _ <-
      ZIO {
        flatResults
          .zipWithIndex
          .map((file, index) =>
            rename(file, index)
          )
      }
    _ <-
      printLine(
        s"Completed with ${files.size} files"
      )
    _ <-
      printLine(
        s"Potential Re-ordering: \n" +
          flatResults.mkString("\n")
      )
  yield ()

@main
def run(args: String*) =
  val f: File =
//    File(args.headOption.getOrElse(""))
    File("Chapters")
  unsafeRun(program(f.getAbsoluteFile.nn))

def rename(original: File, index: Int) =

  val stripped =
    original
      .getName
      .nn
      .dropWhile(_ != '_')
      .drop(1)

  def cleanupName(s: String): String =
    s.stripPrefix("# ")
      .replace(' ', '_')
      .nn
      .replaceAll("[^0-9a-zA-Z_]", "")
      .nn + ".md"

  val fromMarkdown =
    Source
      .fromFile(original)
      .getLines()
      .nextOption()
      .map(cleanupName)
      .getOrElse(stripped)

  val withLeadingZero =
    if (index > 9)
      index.toString
    else
      s"0$index"

  val name = withLeadingZero + "_" + fromMarkdown

  original
    .renameTo(File(original.getParentFile, name))
end rename

def withMarkdownNames(
    files: Seq[File]
): ZIO[Any, FileNotFoundException, Seq[File]] =
  println(files)
  ZIO.succeed(files)
