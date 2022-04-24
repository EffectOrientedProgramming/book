package game_theory

import zio.{Ref, ZIO, ZLayer}

trait DecisionService:
  def getDecisionsFor(
      prisoner1: Prisoner,
      prisoner2: Prisoner
  ): ZIO[Any, String, RoundResult]

object DecisionService:
  class LiveDecisionService(
      history: Ref[DecisionHistory]
  ) extends DecisionService:
    private def getDecisionFor(
        prisoner: Prisoner,
        actionsAgainst: List[Action]
    ): ZIO[Any, String, Decision] =
      for action <-
          prisoner.decide(actionsAgainst)
      yield Decision(prisoner, action)

    def getDecisionsFor(
        prisoner1: Prisoner,
        prisoner2: Prisoner
    ): ZIO[Any, String, RoundResult] =
      for
        prisoner1history <-
          history
            .get
            .map(_.historyFor(prisoner1))
        prisoner2history <-
          history
            .get
            .map(_.historyFor(prisoner2))
        decisions <-
          getDecisionFor(
            prisoner1,
            prisoner2history
          ).zipPar(
            getDecisionFor(
              prisoner2,
              prisoner1history
            )
          )
        roundResult =
          RoundResult(decisions._1, decisions._2)
        _ <-
          history.updateAndGet(oldHistory =>
            DecisionHistory(
              roundResult :: oldHistory.results
            )
          )
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
end DecisionService
