"An Iterator is the fundamental imperative representation of collections of zero or more and potentially infinitely many values. 
Similarly, ZStream is the fundamental functional representation of collections of one or more and potentially infinitely many effectual values."
- Zionomicon

"you can always operate at the level of chunks if you want to. 
But the philosophy of ZIO Stream is that the user should have to manually deal with chunking only as an optimization and 
that in most cases the framework should automatically “do the right thing” with regard to chunking."
- Zionomicon

"in a streaming application implementing a sorted operator would require not only waiting to emit any value until the original stream had terminated, 
which might never happen, but also buffering a potentially unlimited number of prior values, creating a potential memory leak."
- Zionomicon

"""Here are some common collection operators that you can use to transform streams:
    • collect - map and filter stream values at the same time
    • collectWhile - transform stream values as long as the specified partial function is defined
    • concat - pull from the specified stream after this stream is done
    • drop - drop the first n values from the stream
    • dropUntil - drop values from the stream until the specified predicate is true
    • dropWhile - drop values from the stream while the specified predicate is true
    • filter - retain only values satisfying the specified predicate
    • filterNot - drop all values satisfying the specified predicate
    • flatMap - create a new stream for each value and flatten them to a single stream
    • map - transform stream values with the specified function
    • mapAccum - transform stream values with the specified stateful function 
    • scan - fold over the stream values and emit each intermediate result in a new stream
        Bill note - Naively, this one seems dangerous
    • take - take the first n values from the stream
    • takeRight - take the last n values from the stream
    • takeUntil - take values from the stream until the specified predicate is true
    • takeWhile - take values from the stream while the specified predicate is true
    • zip - combine two streams point wise
"""
- Zionomicon

TODO - Should we talk about unfold?
"In addition to the basic variant of unfold there is also an effectual variant, unfoldZIO, 
which allows performing an effect in the state transformation function. 
This allows describing many types of streams, for example reading incrementally from a data source 
while maintaining some cursor that represents where we currently are in reading from the data source."
- Zionomicon

"the signature of the foreach method on ZStream returns a ZIO effect and so it is safe to use. Using this, we can write code like:

    val effect: ZIO[Any, Nothing, Unit] = for {
        x <- ZStream(1, 2)
        y <- ZStream(x, x + 3)
    } Console.printLine((x, y).toString).orDie
This now just describes running these two streams and printing the values they produce to the console."
- Zionomicon


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

import zio._
import zio.stream._

// This currently runs against the dataset available here:
// https://www.kaggle.com/datasets/thoughtvector/customer-support-on-twitter?resource=download
object TwitterCustomerSupport
    extends ZIOAppDefault:
  val fileName = "../datasets/sample.csv"
//    "../datasets/twcs/twcs.csv"
  def run =
    for
      _ <- ZIO.debug("Hi")
      tweetStream =
        ZStream.fromFileName(fileName).drop(1)
      currentLine <-
        Ref.make[Chunk[Byte]](Chunk.empty)
      linesMaybe =
        tweetStream.mapZIO(byte =>
          if (byte == 0xa)
            for
              lineContents <-
                currentLine
                  .getAndSet(Chunk.empty)
              line =
                new String(
                  lineContents
                    .appended(byte)
                    .toArray
                )
//              _ <- ZIO.debug("Line: " + (line))
            yield Some(line)
          else
            currentLine
              .update(_.appended(byte)) *>
              ZIO.succeed(None)
        )
      lines =
        linesMaybe
          .flatMap(o => ZStream.fromIterable(o))
      tweets =
        lines.flatMap(l =>
          ZStream.fromIterable(Tweet(l).toOption)
        )
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
      _ <- companyActivity.debug.runDrain
    yield ()

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

```

            