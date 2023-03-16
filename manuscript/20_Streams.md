# Streams

TODO Prose

## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/streams/Alphabet.scala
```scala
package streams

import zio.*
import zio.stream.*

object Alphabet1 extends ZIOAppDefault:

  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .foreach(ZIO.debug(_))

object Alphabet2 extends ZIOAppDefault:

  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .forever
      .foreach(ZIO.debug(_))

object Alphabet3 extends ZIOAppDefault:

  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .mapZIO { c =>
        val z =
          for
            d <- Random.nextIntBounded(5)
            _ <- ZIO.sleep(d.seconds)
            _ <- ZIO.debug(c)
          yield ()

        z.fork
      }
      .runDrain // exits before all forks are completed

object Alphabet4 extends ZIOAppDefault:
  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .schedule(
        Schedule.fixed(1.second).jittered
      )
      .aggregateAsyncWithin(
        ZSink.collectAll,
        Schedule.fixed(3.seconds)
      )
      .debug("Elements in past 3 seconds")
      .map(_.length)
      .debug("Rate per 3 seconds")
      .runDrain

// doesn't chunk into time-oriented groups as we'd expect
object Alphabet5 extends ZIOAppDefault:
  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .schedule(Schedule.spaced(10.millis))
      .throttleShape(1, 1.second) { chunk =>
        println(chunk)
        1
      }
      .debug
      .runDrain

// grouping as many items as can fit in one second, with a cap of 1000
// Note: Int.MaxValue causes OOM
object Alphabet6 extends ZIOAppDefault:
  def run =
    ZStream
      .fromIterable('a' to 'z')
      .schedule(Schedule.spaced(100.millis))
      .groupedWithin(1000, 1.second)
      .debug
      .runDrain

```


### experiments/src/main/scala/streams/CommitStream.scala
```scala
package streams

import zio.*
import zio.stream.*

trait CommitStream:
  def commits: Stream[Nothing, Commit]

case class Commit(
    project: Project,
    author: Author,
    message: String,
    added: Int,
    removed: Int
)

object CommitStream:
  object Live extends CommitStream:
    def commits: Stream[Nothing, Commit] =
      ZStream.repeatZIO(randomCommit)

  private val randomCommit =
    for
      author       <- Author.random
      project      <- Project.random
      message      <- Message.random
      linesAdded   <- Random.nextIntBounded(500)
      linesRemoved <- Random.nextIntBounded(500)
    yield Commit(
      project,
      author,
      message,
      linesAdded,
      -linesRemoved
    )
end CommitStream

object Message:
  private val generic =
    List(
      "Refactor code",
      "Add documentation",
      "Update dependencies",
      "Format code",
      "Fix bug",
      "Add feature",
      "Add tests",
      "Remove unused code"
    )

  def random: ZIO[Any, Nothing, String] =
    randomElementFrom(generic)

case class Project(
    name: String,
    language: Language
)
object Project:
  private val entries =
    List(
      Project("ZIO", Language.Scala),
      Project("Tapir", Language.Scala),
      Project("Kafka", Language.Java),
      Project("Flask", Language.Python),
      Project("Linux", Language.C)
    )

  val random: ZIO[Any, Nothing, Project] =
    randomElementFrom(entries)

enum Language:
  case Scala,
    Java,
    C,
    CPlusPlus,
    Go,
    Rust,
    Python,
    Unison,
    Ruby

enum Author:
  case Kit,
    Adam,
    Bruce,
    James,
    Bill

object Author:
  val random: ZIO[Any, Nothing, Author] =
    randomElementFrom(Author.values.toList)

```


### experiments/src/main/scala/streams/Counter.scala
```scala
package streams

import zio.{Ref, ZIO}

case class Counter(count: Ref[Int]):
  val get: ZIO[Any, Nothing, Int] =
    count.getAndUpdate(_ + 1)

object Counter:
  val make = Ref.make(0).map(Counter(_))

```


### experiments/src/main/scala/streams/DataFountain.scala
```scala
package streams

import zio.*
import zio.metrics.MetricKeyType.Counter
import zio.stream.*

case class DataFountain(
    tweets: TweetStream,
    commitStream: CommitStream,
    httpRequestStream: HttpRequestStream,
    rate: Schedule[Any, Nothing, Long] =
      Schedule.spaced(1.second)
):
  def withRate(newValue: Int) =
    copy(rate =
      Schedule
        .spaced(1.second.dividedBy(newValue))
    )

object DataFountain:

  def userFriendlyConstructor(rate: Int) =
    DataFountain(
      TweetStream.Live,
      CommitStream.Live,
      HttpRequestStream.Live,
      Schedule.spaced(1.second.dividedBy(rate))
    )

  val live =
    DataFountain(
      TweetStream.Live,
      CommitStream.Live,
      HttpRequestStream.Live
    )

    // TODO More throttle investigation
//      tweets.throttleEnforce(1, 1.second, 1)(_.length)

```


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


### experiments/src/main/scala/streams/DemoDataFountain.scala
```scala
package streams

import zio._

object DemoDataFountain extends ZIOAppDefault:
  def run =
    DataFountain
      .live
      .commitStream
      .commits
      .take(10)
      .foreach(ZIO.debug(_))

object RecognizeBurstOfBadRequests
    extends ZIOAppDefault:
  def run =
    DataFountain
      .live
      .httpRequestStream
      .requests
      .groupedWithin(10, 1.second)
      .debug
      .foreach(requests =>
        ZIO.when(
          requests
            .filter(r =>
              r.response == Code.Forbidden
            )
            .length > 2
        )(ZIO.debug("Too many bad requests"))
      )
      .timeout(5.seconds)
end RecognizeBurstOfBadRequests

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


### experiments/src/main/scala/streams/HttpRequestStream.scala
```scala
package streams

import zio.*
import zio.stream.*
import zio.test.Gen

case class Request(response: Code, path: Path)

trait HttpRequestStream:
  def requests: Stream[Nothing, Request]

object HttpRequestStream:
  object Live extends HttpRequestStream:
    override def requests
        : Stream[Nothing, Request] =
      ZStream
        .repeatZIO(randomRequest)
        .schedule(Schedule.spaced(100.millis))

  private val randomRequest =
    for
      code <- Code.random
      path <- Path.random
    yield Request(code, path)

enum Code:
  case Ok,
    BadRequest,
    Forbidden

object Code:
  val random =
    randomElementFrom(Code.values.toList)

case class Path(segments: Seq[String]):
  override def toString: String =
    segments.mkString("/")

object Path:
  val random: ZIO[Any, Nothing, Path] =
    for
      generator <-
        randomElementFrom(Random.generators)
      path <- generator
    yield path

  def apply(first: String, rest: String*): Path =
    Path(Seq(first) ++ rest)

  private object Random:
    private val generic
        : ZIO[Any, Nothing, Path] =
      val genericPaths =
        List(
          "login",
          "preferences",
          "settings",
          "home",
          "latest",
          "logout"
        )

      for section <-
          randomElementFrom(genericPaths)
      yield Path(s"/$section")

    private val user: ZIO[Any, Nothing, Path] =
      val userSections =
        List(
          "activity",
          "status",
          "collaborators"
        )

      for
        userId <- zio.Random.nextIntBounded(1000)
        section <-
          randomElementFrom(userSections)
      yield Path(s"/user/$userId/$section")

    val generators
        : List[ZIO[Any, Nothing, Path]] =
      List(generic, user)
  end Random

end Path

private[streams] def randomElementFrom[T](
    collection: List[T]
): ZIO[Any, Nothing, T] =
  for idx <-
      Random.nextIntBounded(collection.length)
  yield collection(idx)

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


### experiments/src/main/scala/streams/TweetFactory.scala
```scala
package streams

import zio.{Random, ZIO}

case class TweetFactory(counter: Counter):

  val randomTweet
      : ZIO[Any, Nothing, SimpleTweet] =
    for
      subject   <- TweetFactory.randomSubject
      adjective <- TweetFactory.randomAdjective
      id        <- counter.get
    yield SimpleTweet(
      id,
      s"$subject is the $adjective thing ever!"
    )

private object TweetFactory:
  val make: ZIO[Any, Nothing, TweetFactory] =
    Counter.make.map(TweetFactory(_))

  val superlatives =
    List("best", "greatest", "most awesome")
  val derogatory =
    List("worst", "most terrible", "most awful")

  val allAdjectives = superlatives ++ derogatory
  val allSubjects =
    List(
      "Ice cream",
      "The sunrise",
      "Rain",
      "ZIO",
      "PHP",
      "Skiing",
      "Music"
    )
  val randomAdjective =
    for index <-
        Random.nextIntBounded(allAdjectives.size)
    yield allAdjectives(index)

  val randomSubject =
    for index <-
        Random.nextIntBounded(allSubjects.size)
    yield allSubjects(index)
end TweetFactory

```


### experiments/src/main/scala/streams/TweetStream.scala
```scala
package streams

import zio.*
import zio.stream.*

case class SimpleTweet(id: Int, text: String)

trait TweetStream:
  def tweets: Stream[Nothing, SimpleTweet]
  val slowTweetStream: Stream[
    Nothing,
    SimpleTweet
  ]

object TweetStream:
  object Live extends TweetStream:
    private val tweetService =
      ZLayer.fromZIO(TweetFactory.make)

    private val tweetsPerSecond = 6000
    private val tweetRate =
      Schedule.spaced(
        1.second.dividedBy(tweetsPerSecond)
      )

    val tweets
        : ZStream[Any, Nothing, SimpleTweet] =
      ZStream
        .repeatZIO(
          ZIO.serviceWithZIO[TweetFactory](
            _.randomTweet
          )
        )
        .schedule(tweetRate)
        .provideLayer(tweetService)

    val slowTweetStream =
      tweets.throttleShape(1, 1.second)(_.length)
  end Live
end TweetStream

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

            