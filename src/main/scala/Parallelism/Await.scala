package Parallelism

import java.io.IOException
import zio._
import zio.console._

class Await:

  // Another way of ending a fiber is with the
  // Await method.
  // This will return an effect that represents
  // an Exit value.
  // This exit value will provide the information
  // on how the Fiber ended.

  // This effect, betterBeBanana, has an inner
  // effect, isBanana, that fails if word is not
  // banana.
  // Then betterBeBanana will fork off isBanana,
  // await it, then return its Exit object.
  // The awaited fiber of isBanana will return an
  // exit object. The exit object will display if
  // isBanana succeeded
  // or not, ie if word == banana.

  val word = "Banana"

  val betterBeBanana
      : ZIO[Any, Nothing, Exit[String, String]] = //Will return the exit object of isBanana (on success)

    val isBanana: IO[String, String] = //Will succeed if word is Banana. Will fail otherwise
      if (word == "Banana")
        ZIO.succeed("Good")
      else
        ZIO.fail("Not banana")

    for
      fiber <-
        isBanana.fork //Make a fiber of isBanana
      exit <-
        fiber
          .await //await fiber, get its exit object
    yield exit //return exit object
end Await
