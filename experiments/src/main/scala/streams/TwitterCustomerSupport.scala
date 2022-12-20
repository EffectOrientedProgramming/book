package streams

import zio.*
import zio.stream.*

import java.time.Instant
import java.nio.file.{Files, Paths}

// This currently runs against the dataset available here:
// https://www.kaggle.com/datasets/thoughtvector/customer-support-on-twitter?resource=download
object TwitterCustomerSupport
    extends ZIOAppDefault:
  val fileName =
//    "../datasets/sample.csv"
    "../datasets/twcs/twcs.csv"


  def isHappy(tweet: Tweet): Boolean =
    List("fantastic", "awesome", "great", "wonderful")
      .exists(tweet.text.toLowerCase.contains(_))

  def isAngry(tweet: Tweet): Boolean =
    List("stupid", "dumb", "idiot", "shit")
      .exists(tweet.text.toLowerCase.contains(_))


  def run =
    (for
      lineNumber <- Ref.make(0)
      lines =
        ZStream.fromJavaStream(
          Files.lines(Paths.get("..", "datasets", "twcs", "twcs.csv"))
        )
      tweets =
        lines.map(l =>
          Tweet(l)
        ).filter(_.isRight)
          .map(_.getOrElse(???))
      activeCompanies <-
        Ref.make[Map[String, Int]](Map.empty)
      mostActiveRef <- Ref.make[(String, Int)](("UNKNOWN", 0))

//      mostActiveCompanyAtEachMoment =
//        tweets.mapZIO(tweet =>
//          for
//            companies <-
//              activeCompanies.updateAndGet(
//                incrementCompanyActivity(
//                  _,
//                  tweet
//                )
//              )
//          yield  companies.map(x=>x).toList.sortBy(_._2).reverse // TODO Check Performance of reversing
//        )
//      _ <- mostActiveCompanyAtEachMoment
//        .mapZIO(activities => ZIO.debug(activities.take(3).mkString(" : ")))
//          .runDrain

      _ <- tweets.filter(isHappy(_))
        .runCount
        .debug("Number of happy tweets") <&> tweets.filter(isAngry(_))
        .runCount
        .debug("Number of angry tweets")



    yield ())
      .timeout(30.seconds)

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
