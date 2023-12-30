package concurrency

import zio.Console.printLine

import java.nio.file.Path

// TODO Figure if these functions belong in the object instead.
case class FSLive() extends FileSystem:
  def readFileExpensive(
                         name: Path
                       ): ZIO[Any, Nothing, FileContents] =
    defer:
      printLine("Reading from FileSystem")
        .orDie
        .run

      ZIO.sleep(2.seconds).run
      FSLive.hardcodedFileContents


object FSLive:
  val hardcodedFileContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )
