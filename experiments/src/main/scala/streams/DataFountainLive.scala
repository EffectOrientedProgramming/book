package streams

import zio.*
import zio.metrics.MetricKeyType.Counter
import zio.stream.*

case class SimpleTweet(id: Int, text: String)

case class DataFountain(tweets: TweetStream)
//  def tweets: Stream[Nothing, SimpleTweet]
//  val slowTweetStream: Stream[Nothing, SimpleTweet]

object DataFountain:
  val live =
    DataFountain(
      TweetStream.Live
    )

trait TweetStream:
  def tweets: Stream[Nothing, SimpleTweet]
  val slowTweetStream: Stream[Nothing, SimpleTweet]


object TweetStream:
  object Live extends TweetStream:
    private val tweetsPerSecond = 6000
    private val tweetService = ZLayer.fromZIO(Tweets.make)
    private val tweetRate = Schedule.spaced(1.second.dividedBy(tweetsPerSecond) )

    val tweets: Stream[Nothing, SimpleTweet] =
      ZStream.repeatZIO(ZIO.serviceWithZIO[Tweets](_.randomTweet))
        .schedule(tweetRate)
        .provideLayer(tweetService)

    val slowTweetStream =
      tweets.throttleShape(1, 1.second)(_.length)
      // TODO More throttle investigation
//      tweets.throttleEnforce(1, 1.second, 1)(_.length)

object DemoDataFountain extends ZIOAppDefault:
  def run =
    DataFountain.live.tweets.slowTweetStream
      .take(5)
      .debug
      .runDrain

case class Counter(count: Ref[Int]):
  def get: ZIO[Any, Nothing, Int] = count.getAndUpdate(_ + 1)

object Counter:
  val make =
    Ref.make(0).map(Counter(_))

