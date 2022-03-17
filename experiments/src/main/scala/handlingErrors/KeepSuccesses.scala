package handlingErrors

import zio.ZIO

object KeepSuccesses extends zio.ZIOAppDefault {
  val allCalls =
    List(
      shoddyNetworkCall("a"),
      shoddyNetworkCall("b"),
      shoddyNetworkCall("large payload"),
    )
  val logic =
    ZIO.collectAllSuccesses(
      allCalls.map(_.tapError(e => zio.Console.printLine("Error: " + e))),
    )

  val logicSpecific =
    ZIO.collectAllWith(
      allCalls

    )(_.contains("a"))

  def  run =
    for {
      results <- logic
      _ <-  zio.Console.printLine(results.filter(_.contains("b")).mkString(","))
    } yield ()

  def shoddyNetworkCall(input: String) =
    if (input.length < 5)
      ZIO.succeed("Good call: " + input)
    else
      ZIO.fail("Bad call: " + input)
}
