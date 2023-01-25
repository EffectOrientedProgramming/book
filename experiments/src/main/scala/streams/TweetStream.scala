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
    private val tweetsPerSecond = 6000
    private val tweetService =
      ZLayer.fromZIO(Tweets.make)
    private val tweetRate =
      Schedule.spaced(
        1.second.dividedBy(tweetsPerSecond)
      )

    val tweets: Stream[Nothing, SimpleTweet] =
      ZStream
        .repeatZIO(
          ZIO.serviceWithZIO[Tweets](
            _.randomTweet
          )
        )
        .schedule(tweetRate)
        .provideLayer(tweetService)

    val slowTweetStream =
      tweets.throttleShape(1, 1.second)(_.length)
  end Live
end TweetStream
