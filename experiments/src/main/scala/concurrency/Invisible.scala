package concurrency

import zio.Console.printLine

import java.nio.file.Path

// TODO Figure if these functions belong in the object instead.
case class FSLive(requests: Ref[Int])
    extends CloudStorage:
  def expensiveDownload(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    defer:
      // TODO Delete this when we are confident
      // we only care about the invoice
      printLine("Reading from FileSystem")
        .orDie
        .run
      requests.update(_ + 1).run

      ZIO.sleep(2.seconds).run
      FSLive.hardcodedContents

  val invoice: ZIO[Any, Nothing, String] =
    requests
      .get
      .map(count => "Amount owed: $" + count)
end FSLive

object FSLive:
  val hardcodedContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )
