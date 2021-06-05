package Hubs

object HubExploration extends zio.App {
  import zio._

  def run(args: List[String]) = //Use App's run function
    val logic =
      for
        hub <- Hub.bounded[Int] (2)
        _ <- hub.subscribe.use {
          case hubSubscription =>
            for
              _ <- hub.publish(42)
              _ <- hubSubscription.take.flatMap(currentInt => console.putStrLn("Int: " + currentInt))
            yield ()
        }
      yield ()

    logic.exitCode
}
