package hello_failures

import zio.Console.printLine
import zio.ZIO
import zio.direct.*

object KeepSuccesses extends zio.ZIOAppDefault:
  val allCalls =
    List("a", "b", "large payload", "doomed")

  case class GoodResponse(payload: String)
  case class BadResponse(payload: String)

  val initialRequests =
    allCalls.map(fastUnreliableNetworkCall)

  val logic =
    ZIO
      .collectAllSuccesses(
        initialRequests.map(
          _.tapError(e =>
            printLine("Error: " + e)
          )
        )
      )
      .debug

  val moreStructuredLogic =
    defer {
      val results =
        ZIO
          .partition(allCalls)(
            fastUnreliableNetworkCall
          )
          .run
      results match
        case (failures, successes) =>
          defer {
            ZIO
              .foreach(failures)(e =>
                printLine(
                  "Error: " + e +
                    ". Should retry on other server."
                )
              )
              .run
            val recoveries =
              ZIO
                .collectAllSuccesses(
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
                .run
            printLine(
              "All successes: " +
                (successes ++ recoveries)
            ).run
          }.run
      end match
    }

  def run = moreStructuredLogic

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
