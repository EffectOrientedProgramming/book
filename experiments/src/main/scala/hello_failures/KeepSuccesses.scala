package hello_failures

import zio.Console.printLine

object KeepSuccesses extends zio.ZIOAppDefault:
  val allCalls =
    List("a", "b", "large payload", "doomed")

  case class GoodResponse(payload: String)
  case class BadResponse(payload: String)

  val initialRequests =
    allCalls
      .map:
        fastUnreliableNetworkCall

  val moreStructuredLogic =
    defer:
      val results =
        ZIO
          .partition(allCalls)(
            fastUnreliableNetworkCall
          )
          .run
      results match
        case (failures, successes) =>
          defer:
            printFailures(failures).run
            val recoveries =
              attemptFallbackFor(failures).run
            successes ++ recoveries
          .debug:
            "All successes"
          .run

  def run = moreStructuredLogic

  private def printFailures(
      failures: Iterable[BadResponse]
  ) =
    ZIO.foreach(failures): e =>
      printLine:
        "Error: " + e +
          ". Should retry on other server."

  private def attemptFallbackFor(
      failures: Iterable[BadResponse]
  ) =
    ZIO.collectAllSuccesses:
      failures.map: failure =>
        slowMoreReliableNetworkCall:
          failure.payload
        .tapError: e =>
          printLine:
            "Giving up on: " + e

  def fastUnreliableNetworkCall(input: String) =
    if (input.length < 5)
      ZIO.succeed:
        GoodResponse(input)
    else
      ZIO.fail:
        BadResponse(input)

  def slowMoreReliableNetworkCall(
      input: String
  ) =
    if (input.contains("a"))
      ZIO.succeed:
        GoodResponse(input)
    else
      ZIO.fail:
        BadResponse(input)
      
end KeepSuccesses
