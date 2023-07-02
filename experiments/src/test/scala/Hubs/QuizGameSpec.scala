package Hubs

import Hubs.QuizGame.cahootGame
import zio.*
import zio.test.*
import zio.direct.*

object QuizGameSpec extends ZIOSpecDefault:

  val frop  = Player("Frop")
  val zeb   = Player("Zeb")
  val shtep = Player("Shtep")
  val cheep = Player("Cheep")

  val players: List[Player] =
    List(frop, zeb, shtep, cheep)
  def spec =
    suite("QuizGameSpec")(
      test("roundWithMultipleCorrectAnswers") {
        val roundWithMultipleCorrectAnswers =
          RoundDescription(
            Question(
              "What is the southern-most European country?",
              "Spain"
            ),
            Seq(
              Answer(zeb, "Germany", 2.seconds),
              Answer(frop, "Spain", 1.seconds),
              Answer(cheep, "Spain", 3.seconds),
              Answer(shtep, "Spain", 4.seconds)
            )
          )

        val rounds =
          Seq(roundWithMultipleCorrectAnswers)

        defer {
          val results =
            QuizGame
              .cahootGame(rounds, players)
              .run
          assertTrue(
            results ==
              List(
                RoundResults(List(frop, cheep))
              )
          )
        }
      },
      test("roundWithOnly1CorrectAnswer") {
        val roundWithOnly1CorrectAnswer =
          RoundDescription(
            Question(
              "What is the lightest element?",
              "Hydrogen"
            ),
            Seq(
              Answer(frop, "Lead", 2.seconds),
              Answer(zeb, "Hydrogen", 1.seconds),
              Answer(cheep, "Gold", 3.seconds),
              Answer(
                shtep,
                "Hydrogen",
                10.seconds
              )
            )
          )

        val rounds =
          Seq(roundWithOnly1CorrectAnswer)

        defer {
          val results =
            QuizGame
              .cahootGame(rounds, players)
              .run
          assertTrue(
            results ==
              List(RoundResults(List(zeb)))
          )
        }
      },
      test("roundWhereEverybodyIsWrong") {
        val roundWhereEverybodyIsWrong =
          RoundDescription(
            Question(
              "What is the average airspeed of an unladen swallow?",
              "INSUFFICIENT DATA FOR MEANINGFUL ANSWER"
            ),
            Seq(
              Answer(frop, "3.0 m/s", 1.seconds),
              Answer(zeb, "Too fast", 1.seconds),
              Answer(
                cheep,
                "Not fast enough",
                1.seconds
              ),
              Answer(shtep, "Scary", 1.seconds)
            )
          )

        val rounds =
          Seq(roundWhereEverybodyIsWrong)

        defer {
          val results =
            QuizGame
              .cahootGame(rounds, players)
              .run
          assertTrue(
            results == List(RoundResults(List()))
          )
        }
      }
    ) @@ TestAspect.withLiveClock
end QuizGameSpec
