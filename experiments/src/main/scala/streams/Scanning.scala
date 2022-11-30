package streams

import zio._
import zio.stream._

object Scanning extends ZIOAppDefault:
  enum GdpDirection:
    case GROWING,
      SHRINKING

  enum EconomicStatus:
    case GOOD_TIMES,
      RECESSION

  import GdpDirection._
  import EconomicStatus._

  case class EconomicHistory(
      quarters: Seq[GdpDirection],
      economicStatus: EconomicStatus
  )

  object EconomicHistory:
    def apply(
        quarters: Seq[GdpDirection]
    ): EconomicHistory =
      EconomicHistory(
        quarters,
        if (
          quarters
            .sliding(2)
            .toList
            .lastOption
            .contains(List(SHRINKING, SHRINKING))
        )
          RECESSION
        else
          GOOD_TIMES
      )

  val gdps =
    ZStream(
      GROWING,
      SHRINKING,
      GROWING,
      SHRINKING,
      SHRINKING
    )
  val economicSnapshots =
    gdps.scan(EconomicHistory(List.empty))(
      (history, gdp) =>
        EconomicHistory(history.quarters :+ gdp)
    )
  def run =
    economicSnapshots.runForeach(snapShot =>
      ZIO.debug(snapShot)
    )
end Scanning
