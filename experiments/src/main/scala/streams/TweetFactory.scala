package streams

case class TweetFactory(counter: Counter):

  val randomTweet
      : ZIO[Any, Nothing, SimpleTweet] =
    defer {
      val subject =
        TweetFactory.randomSubject.run
      val adjective =
        TweetFactory.randomAdjective.run
      val id = counter.get.run
      SimpleTweet(
        id,
        s"$subject is the $adjective thing ever!"
      )
    }

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
    defer {
      val index =
        Random
          .nextIntBounded(allAdjectives.size)
          .run
      allAdjectives(index)
    }

  val randomSubject =
    defer {
      val index =
        Random
          .nextIntBounded(allSubjects.size)
          .run
      allSubjects(index)
    }
end TweetFactory
