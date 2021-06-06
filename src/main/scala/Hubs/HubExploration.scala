package Hubs

object HubExploration extends zio.App {
  import zio._

  def run(args: List[String]) = //Use App's run function
    val getAnInt =
      for
        _ <- console.putStrLn("Please provide an int")
        input <- console.getStrLn
      yield input.toInt

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

            for
              _ <- ZIO.collectAllPar(Set(getAndStoreInput, processNextIntAndPrint))
                    .repeatN(3)
            yield ()
        }
      yield ()

    logic.exitCode
}
