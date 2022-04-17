package game_theory

import game_theory.Action.{Betray, Silent}
import game_theory.Outcome.{BothFree, BothPrison, OnePrison}
import zio.Console.printLine
import zio.*

case class Prisoner(name: String)

enum Action:
  case Silent
  case Betray

enum Outcome:
  case BothFree, BothPrison
  case OnePrison(prisoner: Prisoner)

object SingleBasic extends ZIOAppDefault {
  val bruce = Prisoner("Bruce")
  val bill = Prisoner("Bill")

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

  val basicHardcodedService = ZLayer.succeed(new DecisionService {
    def getDecisionFor(prisoner: Prisoner): ZIO[Clock, String, Action] =
      (if ( prisoner == bruce)
        ZIO.succeed(Silent)
      else if (prisoner == bill)
        ZIO.sleep(4.seconds) *> ZIO.succeed(Betray)
      else ZIO.fail("Unknown prisoner")).debug(prisoner.name)
  })

  val everybodyIsAnAsshole = ZLayer.succeed(new DecisionService {
    def getDecisionFor(prisoner: Prisoner): ZIO[Clock, String, Action] =
      ZIO.succeed(Betray)
  })


  def run =
    play(bruce, bill).debug
      .provideSomeLayer[Clock](
        //        basicHardcodedService
        everybodyIsAnAsshole
      )

}
