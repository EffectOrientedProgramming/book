package streams

import zio.stream.*

import java.nio.file.{Files, Paths}

// This currently runs against the dataset available here:
// https://www.kaggle.com/datasets/thoughtvector/customer-support-on-twitter?resource=download
object TwitterCustomerSupport
    extends ZIOAppDefault:
  val fileName =
//    "../datasets/sample.csv"
//    "../datasets/twcs/twcs.csv"
//    "small"
    "medium"
//    "twcs_tiny.csv"

  def isHappy(tweet: Tweet): Boolean =
    List(
      "fantastic",
      "awesome",
      "great",
      "wonderful"
    ).exists(tweet.text.toLowerCase.contains(_))

  def isAngry(tweet: Tweet): Boolean =
    List("stupid", "dumb", "idiot", "shit")
      .exists(tweet.text.toLowerCase.contains(_))

  def trackActiveCompanies(
      tweets: ZStream[Any, Throwable, Tweet]
  ) =
    defer {
      val activeCompanies =
        Ref.make[Map[String, Int]](Map.empty).run
      val mostActiveCompanyAtEachMoment =
        tweets.mapZIO(tweet =>
          defer {
            val companies =
              activeCompanies
                .updateAndGet(
                  incrementCompanyActivity(
                    _,
                    tweet
                  )
                )
                .run
            companies
              .map(x => x)
              .toList
              .sortBy(x => -x._2)
          }
        )
      val res =
        mostActiveCompanyAtEachMoment.runLast.run
      res.get
    }
  end trackActiveCompanies

  override def run =
    defer {
      val dataset =
        ZIOAppArgs
          .getArgs
          .map(_.headOption.getOrElse(fileName))
          .run
      val tweets =
        ZStream
          .fromJavaStream(
            Files.lines(
              Paths.get(
                //                "..",
                "datasets",
                "twcs",
                dataset + ".csv"
              )
            )
          )
          .map(l => Tweet(l))
          .filter(_.isRight)
          .map(_.getOrElse(???))

      val happyTweetFilter: ZPipeline[
        Any,
        Nothing,
        Tweet,
        Tweet
      ] =
        ZPipeline.filter(isHappy)

      val angryTweetFilter: ZPipeline[
        Any,
        Nothing,
        Tweet,
        Tweet
      ] =
        ZPipeline.filter(isAngry)

      (tweets >>> happyTweetFilter)
        .runCount
        .debug("Number of happy tweets")
        .run

      (tweets >>> angryTweetFilter)
        .runCount
        .debug("Number of angry tweets")
        .run

      //      gatherHappyTweets
      //        .timed
      //        .map(_._1)
      //        .debug("Happy duration") <&>
      //        gatherAngryTweets <&>
      trackActiveCompanies(tweets)
        .map(_.take(3).mkString(" : "))
        .debug("ActiveCompanies")
        .timed
        .map(_._1)
        .debug("Active Company duration")
        .run
    }
  end run
//      .timeout(60.seconds)

  private def incrementCompanyActivity(
      value1: Map[String, Int],
      tweet: Tweet
  ): Map[String, Int] =
    value1.updatedWith(tweet.author_id) {
      case Some(value) =>
        Some(value + 1)
      case None =>
        Some(1)
    }

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
      val pieces =
        csvLine.split(",")
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
