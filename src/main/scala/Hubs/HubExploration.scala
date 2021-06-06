package Hubs

object HubExploration extends zio.App {
  import zio._
  import zio.duration._

  def run(args: List[String]) = //Use App's run function
    val getAnInt =
      for
        _ <- console.putStrLn("Please provide an int")
        input <- console.getStrLn.timeout(5.seconds)// .map(_.getOrElse(-1))
      yield input.getOrElse("-1").toInt

    val logic =
      for
        hub <- Hub.bounded[Int] (2)
        _ <- hub.subscribe.use {
          case hubSubscription =>
            val getAndStoreInput =
              for
                nextInt <- getAnInt
                _ <- hub.publish(nextInt)
              yield ()

            val processNextIntAndPrint =
              for
                nextInt <- hubSubscription.take
                _ <- console.putStrLn("Int: " + nextInt)
              yield ()

            val reps = 5
            for
              _ <- ZIO.collectAllPar(Set(getAndStoreInput.repeatN(reps), processNextIntAndPrint.forever))
                .timeout(5.seconds)

            yield ()
        }
      yield ()

    logic.exitCode
}
