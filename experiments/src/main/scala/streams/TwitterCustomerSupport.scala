package streams

import zio._
import zio.stream._

// This currently runs against the dataset available here:
// https://www.kaggle.com/datasets/thoughtvector/customer-support-on-twitter?resource=download
object TwitterCustomerSupport extends ZIOAppDefault:
  def run =
    for
      _ <- ZIO.debug("Hi")
      tweetStream = ZStream.fromFileName("datasets/sample.csv")
      currentLine <- Ref.make[Chunk[Byte]](Chunk.empty)
      _ <- tweetStream.mapZIO(
        byte =>
          if (byte ==  0xA)
            for
              lineContents <- currentLine.getAndSet(Chunk.empty)
              _ <- ZIO.debug("Line: " + (new String(lineContents.appended(byte).toArray)))
            yield ()
          else
            currentLine.update(_.appended(byte))
//            ZIO.debug("TODO Collect bytes here!")
      ).runDrain
//      _ <- tweetStream.foreach( byte => ZIO.when(byte == 0xA)(ZIO.debug("New Line")))
    yield ()
