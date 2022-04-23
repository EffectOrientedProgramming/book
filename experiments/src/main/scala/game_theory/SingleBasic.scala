package game_theory

import game_theory.Action.{Betray, Silent}
import game_theory.Outcome.{
  BothFree,
  BothPrison,
  OnePrison
}
import zio.Console.printLine
import zio.*

case class Decision(
    prisoner: Prisoner,
    action: Action
)
case class RoundResult(
    prisoner1Decision: Decision,
    prisoner2Decision: Decision
)
case class DecisionHistory(
    results: List[RoundResult]
)

trait Strategy:
  def decide(
      decisionHistory: DecisionHistory
  ): ZIO[Any, Nothing, Action]

case class Prisoner(
    name: String,
    strategy: Strategy
):
  def decide(
      decisionHistory: DecisionHistory
  ): ZIO[Any, Nothing, Action] =
    strategy.decide(decisionHistory)

val alwaysBetray =
  new Strategy:
    override def decide(
        decisionHistory: DecisionHistory
    ): ZIO[Any, Nothing, Action] =
      ZIO.succeed(Betray)

val alwaysTrust =
  new Strategy:
    override def decide(
        decisionHistory: DecisionHistory
    ): ZIO[Any, Nothing, Action] =
      ZIO.succeed(Silent)

enum Action:
  case Silent
  case Betray

enum Outcome:
  case BothFree,
    BothPrison
  case OnePrison(prisoner: Prisoner)

object SingleBasic extends ZIOAppDefault:
  val bruce = Prisoner("Bruce", alwaysBetray)
  val bill  = Prisoner("Bill", alwaysTrust)

  trait DecisionService:
    def getDecisionsFor(
        prisoner1: Prisoner,
        prisoner2: Prisoner
    ): ZIO[Any, String, RoundResult]

  def play(
      prisoner1: Prisoner,
      prisoner2: Prisoner
  ): ZIO[DecisionService, String, Outcome] =
    for
      decisionService <-
        ZIO.service[DecisionService]
      roundResult <-
        decisionService
          .getDecisionsFor(prisoner1, prisoner2)

      outcome =
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
    yield outcome

  class LiveDecisionService(
      history: Ref[DecisionHistory]
  ) extends DecisionService:
    private def getDecisionFor(
        prisoner: Prisoner
    ): ZIO[Any, String, Decision] =
      for
        currentHistory <- history.get
        action <- prisoner.decide(currentHistory)
      yield Decision(prisoner, action)

    def getDecisionsFor(
        prisoner1: Prisoner,
        prisoner2: Prisoner
    ): ZIO[Any, String, RoundResult] =
      for
        decisions <-
          getDecisionFor(prisoner1)
            .zipPar(getDecisionFor(prisoner2))
        roundResult =
          RoundResult(decisions._1, decisions._2)
        _ <-
          history
            .updateAndGet(oldHistory =>
              DecisionHistory(
                roundResult :: oldHistory.results
              )
            )
            .debug
      yield roundResult
  end LiveDecisionService

  object LiveDecisionService:
    def make(): ZIO[
      Any,
      Nothing,
      LiveDecisionService
    ] =
      for history <-
          Ref.make(DecisionHistory(List.empty))
      yield LiveDecisionService(history)

  val liveDecisionService: ZLayer[
    Any,
    Nothing,
    LiveDecisionService
  ] = ZLayer.fromZIO(LiveDecisionService.make())

  def run =
    play(bruce, bill)
      .repeatN(2)
      .debug
      .provideLayer(
        //        basicHardcodedService
        liveDecisionService
      )
end SingleBasic
