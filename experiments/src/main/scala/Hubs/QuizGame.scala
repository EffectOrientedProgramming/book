package Hubs

import console.FakeConsole

import java.io.IOException
import zio.*
import zio.direct.*
import zio.Console.printLine

case class Player(name: String)

case class Question(
                     text: String,
                     correctResponse: String
                   )

case class Answer(
                   player: Player,
                   text: String,
                   delay: Duration
                 )

case class RoundDescription(
                             question: Question,
                             answers: Seq[Answer]
                           )


object QuizGame extends zio.ZIOAppDefault:
  def run = // Use App's run function

    /* Teacher --> Questions --> Student1 -->
     * Answers --> Teacher Student2 Student3 */

    val frop  = Player("Frop")
    val zeb   = Player("Zeb")
    val shtep = Player("Shtep")
    val cheep = Player("Cheep")

    val players: List[Player] =
      List(frop, zeb, shtep, cheep)

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
          Answer(shtep, "Hydrogen", 10.seconds)
        )
      )

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
      Seq(
        roundWithOnly1CorrectAnswer,
        roundWhereEverybodyIsWrong
      )

    cahootGame(rounds, players)
  end run

  // TODO Return result that can be tested
  def cahootGame(rounds: Seq[RoundDescription], players: List[Player]) =
    for
      questionHub <- Hub.bounded[Question](1)
      answerHub: Hub[Answer] <-
        Hub.bounded[Answer](players.size)
      _ <-
        questionHub
          .subscribe
          .zip(answerHub.subscribe)
          .flatMap {
            case (
              questions: Dequeue[Question],
              answers: Dequeue[Answer]
              ) =>
              ZIO.foreach(rounds)(roundDescription => playARound(
                roundDescription,
                questionHub,
                questions,
                answerHub,
                answers: Dequeue[Answer]
              )
              )
          }
    yield ()


  private[Hubs] def playARound(
                  roundDescription: RoundDescription,
                  questionHub: Hub[Question],
                  questions: Dequeue[Question],
                  answerHub: Hub[Answer],
                  answers: Dequeue[Answer]
                ) =
    defer {
      val correctRespondents = Ref.make[List[Player]](List.empty).run
      printLine(
        "==============================="
      ).run
      printLine(
        "Question for round: " +
          roundDescription
            .question
            .text
      ).run
      correctRespondents
        .set(List.empty)
        .run
      questionHub.publish(
        roundDescription.question
      ).run
      val question = questions.take.run
      ZIO
        .collectAllPar(
          Seq(
            submitAnswersAfterDelay(
              answerHub,
              roundDescription
                .answers
            ),
            recordCorrectAnswers(
              roundDescription
                .question
                .correctResponse,
              answers,
              correctRespondents
            ).repeat(
              untilWinnersAreFound(
                correctRespondents
              )
            )
          )
        )
        .timeout(4.second)
        .run
      val winners =
        correctRespondents.get.run
      printRoundResults(winners).run
      printLine(
        "==============================="
      ).run
    }

  private def untilWinnersAreFound(
                            correctRespondents: Ref[List[Player]]
                          ) =
    Schedule.recurUntilZIO(_ =>
      correctRespondents.get.map(_.size == 2)
    )


  private def submitAnswersAfterDelay(
                               answerHub: Hub[Answer],
                               answers: Seq[Answer]
                             ) =
    ZIO.foreachParDiscard(answers) {
      answer =>
        defer {
          ZIO.sleep(answer.delay).run
          answerHub.publish(answer).run
        }
    }

  private def printRoundResults(
                         winners: List[Player]
                       ) =
    val finalOutput =
      if (winners.isEmpty)
        "Nobody submitted a correct response"
      else if (winners.size == 2)
        "Winners: " + winners.mkString(",")
      else
        "Winners of incomplete round: " +
          winners.mkString(",")
    printLine(finalOutput)

  private def recordCorrectAnswers(
                            correctAnswer: String,
                            answers: Dequeue[Answer],
                            correctRespondents: Ref[List[Player]]
                          ) =
    defer {
      val answer = answers.take.run
      val output =
        if (answer.text == correctAnswer)
          correctRespondents
            .update(_ :+ answer.player)
            .run
          "Correct response from: " +
            answer.player
        else
          "Incorrect response from: " +
            answer.player
      printLine(output).run
    }


end QuizGame
