import zio.{Console, Has, ZIO}
import zio.Console.*
import zio.Runtime.default.unsafeRun

import java.io.{
  File,
  FileNotFoundException,
  IOException
}
import scala.util.CommandLineParser.FromString
import scala.util.Try

// interestingly these are effects but are not
// wrapped in ZIO
def validateDir(
    dir: File
): ZIO[Any, FileNotFoundException, Unit] =
  if dir.exists() && dir.isDirectory() then
    ZIO.unit
  else
    ZIO.fail(
      FileNotFoundException(
        s"$dir was not found or was not a directory"
      )
    )

def filesInDir(dir: File): Set[File] =
  dir.listFiles().nn.toSet.map(_.nn)

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

def chapterFiles(dir: File): Set[(Int, File)] =
  val files = filesInDir(dir)
  files.flatMap(parseChapter)

def duplicates(
    files: Set[(Int, File)]
): Set[(Int, File)] =
  val justNums = files.toSeq.map(_._1)
  val dups =
    justNums.diff(justNums.distinct).distinct
  files.filter { f =>
    dups.contains(f._1)
  }

def resolveDups(dups: Set[(Int, File)]): ZIO[Has[
  Console
], Throwable, Seq[(Int, File)]] =
  // todo: better way to get indexes?
  val dupsWithIndex =
    dups
      .toIndexedSeq
      .zipWithIndex
      .map { f =>
        f._1 -> (f._2 + 1)
      }

  val something =
    for
      _ <- printLine("Conflict detected:")
      _ <-
        ZIO.foreach(dupsWithIndex) {
          case (f, i) =>
            printLine(s"$i) ${f._2.getName}")
        }
      _ <-
        printLine("\nWhich one should be first:")
      numString <- readLine
      num <-
        ZIO.fromTry(
          Try(Integer.parseInt(numString))
        ) // todo: retry if unparsable
      (firstSeq, rest) =
        dupsWithIndex.partition(_._2 == num)
      first <-
        if firstSeq.size == 1 then
          ZIO.succeed(firstSeq.head)
        else
          ZIO.fail(
            new Exception("Invalid index")
          ) // todo: retry
      resolution <-
        if rest.size > 1 then
          resolveDups(rest.map(_._1).toSet)
        else
          ZIO.succeed(rest.map(_._1))
    yield resolution

  something
end resolveDups

def program(dir: File) =
  for
    _ <- validateDir(dir)
    _ <- printLine(s"Reordering $dir")
    files = chapterFiles(dir)
    dups  = duplicates(files)
    resolutions <- resolveDups(dups)
    // _ <- reorderFiles(resolutions, files)
    _ <-
      printLine(
        s"Completed with ${files.size} files"
      )
  yield ()

@main
def run(args: String*) =
  val f: File =
    File(args.headOption.getOrElse(""))
  unsafeRun(program(f.getAbsoluteFile.nn))
