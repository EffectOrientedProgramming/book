package streams

import zio._
import zio.stream._

// This currently runs against the dataset available here:
// https://www.kaggle.com/datasets/thoughtvector/customer-support-on-twitter?resource=download
object TwitterCustomerSupport extends ZIOAppDefault:
  def run =
    for
      _ <- ZIO.debug("Hi")
      tweetStream = ZStream.fromFileName("../datasets/sample.csv").drop(1)
      currentLine <- Ref.make[Chunk[Byte]](Chunk.empty)
      linesMaybe = tweetStream.mapZIO(
        byte =>
          if (byte ==  0xA)
            for
              lineContents <- currentLine.getAndSet(Chunk.empty)
              line = new String(lineContents.appended(byte).toArray)
//              _ <- ZIO.debug("Line: " + (line))
            yield Some(line)
          else
            currentLine.update(_.appended(byte)) *> ZIO.succeed(None)

//            ZIO.debug("TODO Collect bytes here!")
      )
      lines = linesMaybe.flatMap(o => ZStream.fromIterable(o))
      tweets = lines.flatMap(l => ZStream.fromIterable(Tweet(l).toOption))
      _ <- tweets.debug.runDrain
//      _ <- lines.foreach(l => ZIO.debug("Line: " + Tweet(l)))
//      _ <- tweetStream.foreach( byte => ZIO.when(byte == 0xA)(ZIO.debug("New Line")))
    yield ()

  case class ParsingError(msg: String)
  case class Tweet(tweet_id: String, author_id: String, inbound: Boolean, created_at: String, text: String, response_tweet_id: Option[String], in_response_to_tweet_id: Option[String])
  object Tweet:
    def apply(csvLine: String): Either[ParsingError, Tweet] =
      val pieces = csvLine.split(",")
      Either.cond(
        pieces.length == 7,
        pieces match
          case Array(tweet_id, author_id, inbound, created_at, text, response_tweet_id, in_response_to_tweet_id) =>
            Tweet(
                tweet_id, author_id, inbound == "True", created_at, text, Some(response_tweet_id), Some(in_response_to_tweet_id)
            )
          case _ => ???
        ,
       ParsingError("Bad value: " + pieces)
    )
