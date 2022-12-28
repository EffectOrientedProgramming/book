

## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/streams/DeliveryCenter.scala
```scala
package streams

import zio.*
import zio.stream.*

case class Order()

/** Possible stages to demo:
  *   1. Ship individual orders as they come 2.
  *      Queue up multiple items and then send 3.
  *      Ship partially-filled truck if it has
  *      been waiting too long
  */
object DeliveryCenter extends ZIOAppDefault:
  sealed trait Truck

  case class TruckInUse(
      queued: List[Order],
      fuse: Promise[Nothing, Unit],
      capacity: Int = 3
  ) extends Truck:
    val isFull: Boolean =
      queued.length == capacity

    val waitingTooLong =
      fuse.isDone.map(done => !done)

  def handle(
      order: Order,
      staged: Ref[Option[TruckInUse]]
  ) =
    def shipIt(reason: String) =
      ZIO.debug(reason + " Ship the orders!") *>
        staged
          .get
          .flatMap(_.get.fuse.succeed(())) *>
        // TODO Should complete latch here before
        // clearing out value
        staged.set(None)

    val loadTruck =
      for
        latch <- Promise.make[Nothing, Unit]
        truck <-
          staged
            .updateAndGet(truck =>
              truck match
                case Some(t) =>
                  Some(
                    t.copy(queued =
                      t.queued :+ order
                    )
                  )
                case None =>
                  Some(
                    TruckInUse(
                      List(order),
                      latch
                    )
                  )
            )
            .map(_.get)
        _ <-
          ZIO.debug(
            "Loading order: " +
              truck.queued.length + "/" +
              truck.capacity
          )
      yield truck

    def shipIfWaitingTooLong(truck: TruckInUse) =
      ZIO
        .whenZIO(truck.waitingTooLong)(
          shipIt(reason =
            "Truck has bit sitting half-full too long."
          )
        )
        .delay(4.seconds)

    for
      truck <- loadTruck
      _ <-
        if (truck.isFull)
          shipIt(reason = "Truck is full.")
        else
          ZIO
            .when(truck.queued.length == 1)(
              ZIO.debug(
                "Adding timeout daemon"
              ) *> shipIfWaitingTooLong(truck)
            )
            .forkDaemon
    yield ()
  end handle

  def run =
    for
      stagedItems <-
        Ref.make[Option[TruckInUse]](None)
      orderStream =
        ZStream.repeatWithSchedule(
          Order(),
          Schedule
            .exponential(1.second, factor = 1.8)
        )
      _ <-
        orderStream
          .foreach(handle(_, stagedItems))
          .timeout(12.seconds)
    yield ()
end DeliveryCenter

```


### experiments/src/main/scala/streams/HelloStreams.scala
```scala
package streams

import zio.*
import zio.stream.*

object HelloStreams extends ZIOAppDefault:
  def run =
    for
      _ <- ZIO.debug("Stream stuff!")
      greetingStream =
        ZStream.repeatWithSchedule(
          "Hi",
          Schedule.spaced(1.seconds)
        )
      insultStream =
        ZStream.repeatWithSchedule(
          "Dummy",
          Schedule.spaced(2.seconds)
        )
      combinedStream =
        ZStream.mergeAllUnbounded()(
          greetingStream,
          insultStream
        )
      aFewElements = combinedStream.take(6)
      res <- aFewElements.runCollect
      _   <- ZIO.debug("Res: " + res)
    yield ()
end HelloStreams

```


### experiments/src/main/scala/streams/MultipleConcurrentStreams.scala
```scala
package streams

import zio.*
import zio.stream.*

import java.io.File

object MultipleConcurrentStreams
    extends ZIOAppDefault:
  val userActions =
    ZStream(
      "login",
      "post:I feel happy",
      "post: I want to buy something!",
      "updateAccount",
      "logout",
      "post:I want to buy something expensive"
    ).mapZIO(action =>
      ZIO.sleep(1.seconds) *> ZIO.succeed(action)
    )
//      .throttleShape(1, 1.seconds, 2)(_.length)

  // Note: I tried to bake this into the mapZIO
  // call above, but that resulted in additional
  // printing
  // for every consumer of the stream.
  // Surprising, but I'm sure there's good
  // reasoning behind it.
  val userActionAnnouncements =
    userActions.mapZIO(action =>
      ZIO.debug("Incoming event: " + action)
    )

  val actionBytes: ZStream[Any, Nothing, Byte] =
    userActions.flatMap(action =>
      ZStream
        .fromIterable((action + "\n").getBytes)
    )
  val filePipeline
      : ZPipeline[Any, Throwable, Byte, Long] =
    ZPipeline.fromSink(
      ZSink.fromFile(new File("target/output"))
    )
  val writeActionsToFile =
    actionBytes >>> filePipeline

  val marketingData =
    userActions
      .filter(action => action.contains("buy"))

  val marketingActions =
    marketingData.mapZIO(marketingDataPoint =>
      ZIO.debug(
        "  $$ info: " + marketingDataPoint
      )
    )

  val accountAuthentication =
    userActions.filter(action =>
      action == "login" || action == "logout"
    )

  val auditingReport =
    accountAuthentication.mapZIO(event =>
      ZIO.debug("  Security info: " + event)
    )

  def run =
    ZStream
      .mergeAllUnbounded()(
        userActionAnnouncements,
        marketingActions,
        auditingReport,
        writeActionsToFile
      )
      .runDrain
end MultipleConcurrentStreams

```


### experiments/src/main/scala/streams/Scanning.scala
```scala
package streams

import zio._
import zio.stream._

object Scanning extends ZIOAppDefault:
  enum GdpDirection:
    case GROWING,
      SHRINKING

  enum EconomicStatus:
    case GOOD_TIMES,
      RECESSION

  import GdpDirection._
  import EconomicStatus._

  case class EconomicHistory(
      quarters: Seq[GdpDirection],
      economicStatus: EconomicStatus
  )

  object EconomicHistory:
    def apply(
        quarters: Seq[GdpDirection]
    ): EconomicHistory =
      EconomicHistory(
        quarters,
        if (
          quarters
            .sliding(2)
            .toList
            .lastOption
            .contains(List(SHRINKING, SHRINKING))
        )
          RECESSION
        else
          GOOD_TIMES
      )

  val gdps =
    ZStream(
      GROWING,
      SHRINKING,
      GROWING,
      SHRINKING,
      SHRINKING
    )
  val economicSnapshots =
    gdps.scan(EconomicHistory(List.empty))(
      (history, gdp) =>
        EconomicHistory(history.quarters :+ gdp)
    )
  def run =
    economicSnapshots.runForeach(snapShot =>
      ZIO.debug(snapShot)
    )
end Scanning

```


### experiments/src/main/scala/streams/TwitterCustomerSupport.scala
```scala
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
//    "../datasets/twcs/twcs.csv"
    "twcs_small.csv"
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
    for
      activeCompanies <-
        Ref.make[Map[String, Int]](Map.empty)
      mostActiveCompanyAtEachMoment =
        tweets.mapZIO(tweet =>
          for companies <-
              activeCompanies.updateAndGet(
                incrementCompanyActivity(
                  _,
                  tweet
                )
              )
          yield companies
            .map(x => x)
            .toList
            .sortBy(x => -x._2)
        )
      res <-
        mostActiveCompanyAtEachMoment.runLast
    yield res.get

  def run =
    for
      dataset <-
        ZIOAppArgs
          .getArgs
          .map(_.headOption.getOrElse(fileName))
      tweets =
        ZStream
          .fromJavaStream(
            Files.lines(
              Paths.get(
                "..",
                "datasets",
                "twcs",
                dataset + ".csv"
              )
            )
          )
          .map(l => Tweet(l))
          .filter(_.isRight)
          .map(_.getOrElse(???))

      happyTweetFilter: ZPipeline[
        Any,
        Nothing,
        Tweet,
        Tweet
      ] = ZPipeline.filter(isHappy)
      angryTweetFilter: ZPipeline[
        Any,
        Nothing,
        Tweet,
        Tweet
      ] = ZPipeline.filter(isAngry)

      gatherHappyTweets =
        (tweets >>> happyTweetFilter)
          .runCount
          .debug("Number of happy tweets")
      gatherAngryTweets =
        (tweets >>> angryTweetFilter)
          .runCount
          .debug("Number of angry tweets")

      _ <-
      gatherHappyTweets
        .timed
        .map(_._1)
        .debug("Happy duration") <&>
        gatherAngryTweets <&>
        trackActiveCompanies(tweets)
          .map(_.take(3).mkString(" : "))
          .debug("ActiveCompanies")
          .timed
          .map(_._1)
          .debug("Active Company duration")
    yield ()
//      .timeout(60.seconds)

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

```

            