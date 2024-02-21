package game_theory

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
      defer {
        val action =
          prisoner.decide(actionsAgainst).run
        Decision(prisoner, action)
      }

    def getDecisionsFor(
        prisoner1: Prisoner,
        prisoner2: Prisoner
    ): ZIO[Any, String, RoundResult] =
      defer {
        val prisoner1history =
          history
            .get
            .map(_.historyFor(prisoner1))
            .run
        val prisoner2history =
          history
            .get
            .map(_.historyFor(prisoner2))
            .run
        val decisions =
          getDecisionFor(
            prisoner1,
            prisoner2history
          ).zipPar(
              getDecisionFor(
                prisoner2,
                prisoner1history
              )
            )
            .run
        val roundResult =
          RoundResult(decisions._1, decisions._2)
        history
          .update(oldHistory =>
            DecisionHistory(
              roundResult :: oldHistory.results
            )
          )
          .run
        roundResult
      }
  end LiveDecisionService

  object LiveDecisionService:
    def make(): ZIO[
      Any,
      Nothing,
      LiveDecisionService
    ] =
      defer {
        val history =
          Ref
            .make(DecisionHistory(List.empty))
            .run
        LiveDecisionService(history)
      }

  val liveDecisionService: ZLayer[
    Any,
    Nothing,
    LiveDecisionService
  ] =
    ZLayer.fromZIO(LiveDecisionService.make())
end DecisionService
