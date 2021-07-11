package Hubs

import fakeEnvironmentInstances.FakeConsole

import java.io.IOException
import zio.{Hub, Ref, Schedule, ZDequeue, ZIO}
import zio.duration.{Duration, durationInt}
import zio.clock.Clock
import zio.console.{Console, putStrLn}

object QuizGame extends zio.App {
  case class Player(name: String)

  case class Question(text: String, correctResponse: String)

  case class Answer(player: Player, text: String, delay: Duration)

  case class RoundDescription(question: Question, answers: Seq[Answer])

  def run(args: List[String]) = //Use App's run function

    /*
      Teacher --> Questions --> Student1 --> Answers --> Teacher
                                Student2
                                Student3
     */

    val frop = Player("Frop")
    val zeb = Player("Zeb")
    val shtep = Player("Shtep")
    val cheep = Player("Cheep")

    val students: List[Player] =
      List(
        frop,
        zeb,
        shtep,
        cheep
      )

    def submitAnswersAfterDelay(answerHub: Hub[Answer], answers: Seq[Answer]) =
      ZIO
        .collectAllPar(
          answers.map { case answer =>
            for
              _ <- ZIO.sleep(answer.delay)
              _ <- answerHub.publish(answer)
            yield ()
          }
        )
        .map(_ =>
          ()
        ) // TODO Ugh. Try to find a better way to ignore/unify this result/type.

    def recordCorrectAnswers(
        correctAnswer: String,
        answers: ZDequeue[Any, Nothing, Answer],
        correctRespondants: Ref[List[Player]]
    ) =
      for // gather answers until there's a winner
        answer <- answers.take
        output <-
          if (answer.text == correctAnswer)
            for
              currentCorrectRespondents <- correctRespondants.get
              _ <- correctRespondants
                .set(currentCorrectRespondents :+ answer.player)
            yield ("Correct response from: " + answer.player)
          else
            ZIO.succeed("Incorrect response from: " + answer.player)
        _ <- putStrLn(output)
      yield ()

    def untilWinnersAreFound(correctRespondants: Ref[List[Player]]) =
      Schedule
        .recurUntilM(_ => correctRespondants.get.map(_.size == 2))

    def printRoundResults(winners: List[Player]) =
      val finalOutput =
        if (winners.isEmpty)
          "Nobody submitted a correct response"
        else if (winners.size == 2)
          "Winners: " + winners.mkString(",")
        else
          "Winners of incomplete round: " + winners.mkString(",")
      putStrLn(finalOutput)

    val round1 =
      RoundDescription(
        Question("What is the southern-most European country?", "Spain"),
        Seq(
          Answer(zeb, "Germany", 2.seconds),
          Answer(frop, "Spain", 1.seconds),
          Answer(cheep, "Spain", 3.seconds),
          Answer(shtep, "Spain", 4.seconds)
        )
      )

    val round2 =
      RoundDescription(
        Question("What is the lightest element?", "Hydrogen"),
        Seq(
          Answer(frop, "Lead", 2.seconds),
          Answer(zeb, "Hydrogen", 1.seconds),
          Answer(cheep, "Gold", 3.seconds),
          Answer(shtep, "Hydrogen", 10.seconds)
        )
      )

    val round3 =
      RoundDescription(
        Question(
          "What is the average airspeed of an unladen swallow?",
          "INSUFFICENT DATA FOR MEANINGFUL ANSWER"
        ),
        Seq(
          Answer(frop, "3.0 m/s", 1.seconds),
          Answer(zeb, "Too fast", 1.seconds),
          Answer(cheep, "Not fast enough", 1.seconds),
          Answer(shtep, "Scary", 1.seconds)
        )
      )

    val rounds = Seq(round1, round2, round3)

    val cahootSingleRound =
      for
        questionHub <- Hub.bounded[Question](1)
        answerHub: Hub[Answer] <- Hub.bounded[Answer](students.size)
        correctRespondants: Ref[List[Player]] <- Ref
          .make[List[Player]](List.empty)
        _ <- questionHub.subscribe.zip(answerHub.subscribe).use {
          case (
                questions,
                answers: ZDequeue[Any, Nothing, Answer]
              ) => {

            def playARound(roundDescription: RoundDescription) =
              for
                _ <- putStrLn("===============================")
                _ <- putStrLn(
                  "Question for round: " + roundDescription.question.text
                )
                _ <- correctRespondants.set(List.empty)
                _ <- questionHub.publish(
                  roundDescription.question
                )
                question <- questions.take
                _ <- ZIO
                  .collectAllPar(
                    Seq(
                      submitAnswersAfterDelay(
                        answerHub,
                        roundDescription.answers
                      ),
                      recordCorrectAnswers(
                        roundDescription.question.correctResponse,
                        answers,
                        correctRespondants
                      )
                        .repeat(
                          untilWinnersAreFound(correctRespondants)
                        )
                    )
                  )
                  .timeout(
                    4.second
                  )
                winners <- correctRespondants.get
                _ <- printRoundResults(winners)
                _ <- putStrLn("===============================")
              yield ()

            ZIO.foreach(rounds)(playARound)
            //            rounds.foreach(playARound)
            //            playARound(round1)
          }
        }
      yield ()

    cahootSingleRound.exitCode
}
