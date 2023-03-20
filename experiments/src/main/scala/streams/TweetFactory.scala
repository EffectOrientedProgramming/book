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
