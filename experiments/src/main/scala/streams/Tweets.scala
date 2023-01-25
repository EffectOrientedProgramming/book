package streams

import zio.{Random, ZIO}

case class Tweets(counter: Counter):

  val randomTweet
      : ZIO[Any, Nothing, SimpleTweet] =
    for
      adjective <- Tweets.randomAdjective
      id        <- counter.get
    yield SimpleTweet(
      id,
      s"ZIO is the $adjective thing ever!"
    )

private object Tweets:
  val make: ZIO[Any, Nothing, Tweets] =
    Counter.make.map(Tweets(_))

  val superlatives =
    List("best", "greatest", "most awesome")
  val derogatory =
    List("worst", "most terrible", "most awful")
  val allAdjectives = superlatives ++ derogatory
  val randomAdjective =
    for index <-
        Random.nextIntBounded(allAdjectives.size)
    yield allAdjectives(index)
