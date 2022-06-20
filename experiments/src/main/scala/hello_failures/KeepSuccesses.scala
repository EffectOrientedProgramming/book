package hello_failures

import zio.Console.printLine
import zio.ZIO

object KeepSuccesses extends zio.ZIOAppDefault:
  val allCalls =
    List("a", "b", "large payload", "doomed")

  case class GoodResponse(payload: String)
  case class BadResponse(payload: String)

  val initialRequests =
    allCalls.map(fastUnreliableNetworkCall)

  val logic =
    for
      results <-
        ZIO.collectAllSuccesses(
          initialRequests.map(
            _.tapError(e =>
              printLine("Error: " + e)
            )
          )
        )
      _ <- printLine(results)
    yield ()

  val moreStructuredLogic =
    for
      results <-
        ZIO.partition(allCalls)(
          fastUnreliableNetworkCall
        )
      _ <-
        results match
          case (failures, successes) =>
            for
              _ <-
                ZIO.foreach(failures)(e =>
                  printLine(
                    "Error: " + e +
                      ". Should retry on other server."
                  )
                )
              recoveries <-
                ZIO.collectAllSuccesses(
                  failures.map(failure =>
                    slowMoreReliableNetworkCall(
                      failure.payload
                    ).tapError(e =>
                      printLine(
                        "Giving up on: " + e
                      )
                    )
                  )
                )
              _ <-
                printLine(
                  "All successes: " +
                    (successes ++ recoveries)
                )
            yield ()
    yield ()

  val logicSpecific =
    ZIO.collectAllWith(initialRequests)(
      _.payload.contains("a")
    )

  def run =
//      logic
    moreStructuredLogic

  def fastUnreliableNetworkCall(input: String) =
    if (input.length < 5)
      ZIO.succeed(GoodResponse(input))
    else
      ZIO.fail(BadResponse(input))

  def slowMoreReliableNetworkCall(
      input: String
  ) =
    if (input.contains("a"))
      ZIO.succeed(GoodResponse(input))
    else
      ZIO.fail(BadResponse(input))
end KeepSuccesses
