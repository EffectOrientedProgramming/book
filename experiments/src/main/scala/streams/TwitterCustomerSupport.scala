package streams

import zio.*
import zio.stream.*

import java.time.Instant

// This currently runs against the dataset available here:
// https://www.kaggle.com/datasets/thoughtvector/customer-support-on-twitter?resource=download
object TwitterCustomerSupport
    extends ZIOAppDefault:
  val fileName =
//    "../datasets/sample.csv"
    "../datasets/twcs/twcs.csv"

  def run =
    for
      startTime <- Clock.instant
      tweetStream =
        ZStream.fromFileName(fileName).drop(1)
      lineNumber <- Ref.make(0)
      currentLine <-
        Ref.make[Chunk[Byte]](Chunk.empty)
      linesMaybe =
        tweetStream.mapZIO(byte =>
              gatherLines(byte, currentLine, lineNumber, startTime)
        )
      lines =
        linesMaybe
          .filter(_.isDefined)
          .map(_.get)
//        linesMaybe
//          .flatMap(o => ZStream.fromIterable(o))
      tweets =
        lines.flatMap(l =>
          ZStream.fromIterable(Tweet(l).toOption)
        )
        lines.map(l =>
          Tweet(l)
        ).filter(_.isRight)
          .map(_.getOrElse(???))
      activeCompanies <-
        Ref.make[Map[String, Int]](Map.empty)
      companyActivity =
        tweets.mapZIO(tweet =>
          for companies <-
              activeCompanies.updateAndGet(
                incrementCompanyActivity(
                  _,
                  tweet
                )
              )
          yield companies.maxBy(_._2)
        )
      _ <-
        companyActivity
          .tap( (name, count) =>
            ZIO.when(count % 100 == 0)(
              ZIO.debug("Active company: " + name + "  " + count + " interactions.")
            )
          )
//        .debug
//        lines
//        linesMaybe
        .runDrain
        .timeout(30.seconds)
    yield ()

  def gatherLines(byte: Byte, currentLine: Ref[Chunk[Byte]], lineNumber: Ref[Int], startTime: Instant) =
    if (byte == 0xa)
      for
        lineContents <-
          currentLine
            .getAndSet(Chunk.empty)
        currentLineNumber <- lineNumber.updateAndGet(_ + 1)
        _ <- ZIO.when(currentLineNumber % 10000 == 0)(
          for
            currentTime <- Clock.instant
            runtime = java.time.Duration.between(startTime, currentTime)
            _ <- ZIO.debug("Line number: " + currentLineNumber + ". Time per 10,000 rows: " + runtime)
          yield ()
        )
        line =
          new String(
            lineContents
              .appended(byte)
              .toArray
          )
      yield Some(line)
    else
      currentLine
        .update(_.appended(byte)) *>
        ZIO.succeed(None)

  private def incrementCompanyActivity(
      value1: Map[String, Int],
      tweet: Tweet
  ): Map[String, Int] =
    value1.updatedWith(tweet.author_id)(entry =>
      entry match
        case Some(value) =>
          Some(value + 1)
        case None =>
          Some(1)
    )

  case class CompanyActivities(
      name: String,
      count: Int
  ) // TODO Use?

  case class ParsingError(msg: String)
  case class Tweet(
      tweet_id: String,
      author_id: String,
      inbound: Boolean,
      created_at: String,
      text: String,
      response_tweet_id: Option[String],
      in_response_to_tweet_id: Option[String]
  )
  object Tweet:
    def apply(
        csvLine: String
    ): Either[ParsingError, Tweet] =
      val pieces = csvLine.split(",")
      Either.cond(
        pieces.length == 7,
        pieces match
          case Array(
                tweet_id,
                author_id,
                inbound,
                created_at,
                text,
                response_tweet_id,
                in_response_to_tweet_id
              ) =>
            Tweet(
              tweet_id,
              author_id,
              inbound == "True",
              created_at,
              text,
              Some(response_tweet_id),
              Some(in_response_to_tweet_id)
            )
          case _ =>
            ???
        ,
        ParsingError("Bad value: " + pieces)
      )
    end apply
  end Tweet
end TwitterCustomerSupport
