package game_theory

import game_theory.Action.{Betray, Silent}
import game_theory.Outcome.{BothFree, BothPrison, OnePrison}
import zio.Console.printLine
import zio.*


trait Strategy:
  def decide(): ZIO[Any, Nothing, Action]

case class Prisoner(name: String, strategy: Strategy):
  def decide(): ZIO[Any, Nothing, Action] = strategy.decide()

val alwaysBetray = new Strategy:
  override def decide(): ZIO[Any, Nothing, Action] = ZIO.succeed(Betray)

val alwaysTrust = new Strategy:
  override def decide(): ZIO[Any, Nothing, Action] = ZIO.succeed(Silent)

enum Action:
  case Silent
  case Betray

enum Outcome:
  case BothFree, BothPrison
  case OnePrison(prisoner: Prisoner)

object SingleBasic extends ZIOAppDefault {
  val bruce = Prisoner("Bruce", alwaysBetray)
  val bill = Prisoner("Bill", alwaysTrust)

  trait DecisionService:
    def getDecisionFor(prisoner: Prisoner): ZIO[Clock, String, Action]


  def play(prisoner1: Prisoner, prisoner2: Prisoner): ZIO[DecisionService with Clock, String, Outcome] =
    for
      decisionService <- ZIO.service[DecisionService]
      decisions <-
        decisionService.getDecisionFor(prisoner1).zipPar(
          decisionService.getDecisionFor(prisoner2))

      outcome = decisions match
        case (Silent, Silent) => BothFree
        case (Betray, Silent) => OnePrison(prisoner2)
        case (Silent, Betray) => OnePrison(prisoner1)
        case (Betray, Betray) => BothPrison
    yield outcome

  val everybodyIsAnAsshole = ZLayer.succeed(new DecisionService {
    def getDecisionFor(prisoner: Prisoner): ZIO[Clock, String, Action] =
      ZIO.succeed(Betray)
  })

  val liveDecisionService = ZLayer.succeed(new DecisionService {
    def getDecisionFor(prisoner: Prisoner): ZIO[Clock, String, Action] =
      prisoner.decide()
  })


  def run =
    play(bruce, bill).debug
      .provideSomeLayer[Clock](
        //        basicHardcodedService
        liveDecisionService
      )

}
