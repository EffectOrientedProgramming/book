package game_theory

import game_theory.Action.{Betray, Silent}
import game_theory.Outcome.{
  BothFree,
  BothPrison,
  OnePrison
}

case class Decision(
    prisoner: Prisoner,
    action: Action
)
case class RoundResult(
    prisoner1Decision: Decision,
    prisoner2Decision: Decision
):
  override def toString: String =
    s"RoundResult(${prisoner1Decision.prisoner}:${prisoner1Decision
        .action} ${prisoner2Decision.prisoner}:${prisoner2Decision.action})"
case class DecisionHistory(
    results: List[RoundResult]
):
  def historyFor(
      prisoner: Prisoner
  ): List[Action] =
    results.map(roundResult =>
      if (
        roundResult.prisoner1Decision.prisoner ==
          prisoner
      )
        roundResult.prisoner1Decision.action
      else
        roundResult.prisoner2Decision.action
    )

trait Strategy:
  def decide(
      actionsAgainst: List[Action]
  ): ZIO[Any, Nothing, Action]

case class Prisoner(
    name: String,
    strategy: Strategy
):
  def decide(
      actionsAgainst: List[Action]
  ): ZIO[Any, Nothing, Action] =
    strategy.decide(actionsAgainst)

  override def toString: String =
    s"$name"

val silentAtFirstAndEventuallyBetray =
  new Strategy:
    override def decide(
        actionsAgainst: List[Action]
    ): ZIO[Any, Nothing, Action] =
      if (actionsAgainst.length < 3)
        ZIO.succeed(Silent)
      else
        ZIO.succeed(Betray)

val alwaysTrust =
  new Strategy:
    override def decide(
        actionsAgainst: List[Action]
    ): ZIO[Any, Nothing, Action] =
      ZIO.succeed(Silent)

val silentUntilBetrayed =
  new Strategy:
    override def decide(
        actionsAgainst: List[Action]
    ): ZIO[Any, Nothing, Action] =
      if (actionsAgainst.contains(Betray))
        ZIO.succeed(Betray)
      else
        ZIO.succeed(Silent)

enum Action:
  case Silent
  case Betray

enum Outcome:
  case BothFree,
    BothPrison
  case OnePrison(prisoner: Prisoner)

object SingleBasic extends ZIOAppDefault:

  def play(
      prisoner1: Prisoner,
      prisoner2: Prisoner
  ): ZIO[DecisionService, String, Outcome] =
    defer {
      val decisionService =
        ZIO.service[DecisionService].run
      val roundResult =
        decisionService
          .getDecisionsFor(prisoner1, prisoner2)
          .debug("Decisions")
          .run

      (
        roundResult.prisoner1Decision.action,
        roundResult.prisoner2Decision.action
      ) match
        case (Silent, Silent) =>
          BothFree
        case (Betray, Silent) =>
          OnePrison(prisoner2)
        case (Silent, Betray) =>
          OnePrison(prisoner1)
        case (Betray, Betray) =>
          BothPrison
    }

  val bruce =
    Prisoner(
      "Bruce",
      silentAtFirstAndEventuallyBetray
    )
  val bill =
    Prisoner("Bill", silentUntilBetrayed)

  def run =
    play(bruce, bill)
      .debug("Outcome")
      .repeatN(4)
      .provideLayer(
        DecisionService.liveDecisionService
      )
end SingleBasic
