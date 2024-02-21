package Hubs

import zio.Console.printLine

import java.io.IOException

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

case class RoundResults(
    correctRespondents: List[Player]
)

object QuizGame:

  // TODO Return result that can be tested
  def cahootGame(
      rounds: Seq[RoundDescription],
      players: List[Player]
  ) =
    defer {
      val questionHub =
        Hub.bounded[Question](1).run
      val answerHub: Hub[Answer] =
        Hub.bounded[Answer](players.size).run
      val (
        questions: Dequeue[Question],
        answers: Dequeue[Answer]
      ) =
        questionHub
          .subscribe
          .zip(answerHub.subscribe)
          .run
      ZIO
        .foreach(rounds)(roundDescription =>
          questionHub.publish(
            roundDescription.question
          ) *>
            playARound(
              roundDescription,
              questions,
              answerHub,
              answers
            )
        )
        .run
    }

  private[Hubs] def playARound(
      roundDescription: RoundDescription,
      questions: Dequeue[Question],
      answerHub: Hub[Answer],
      answers: Dequeue[Answer]
  ): ZIO[Any, IOException, RoundResults] =
    defer {
      val correctRespondents =
        Ref.make[List[Player]](List.empty).run

      printLine(
        "Question for round: " +
          roundDescription.question.text
      ).run

      // TODO This should happen *before*
      // playARound is invoked
      questions.take.run
      ZIO
        .collectAllPar(
          Seq(
            submitAnswersAfterDelay(
              answerHub,
              roundDescription.answers
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

      RoundResults(correctRespondents.get.run)
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
    ZIO.foreachParDiscard(answers) { answer =>
      defer {
        ZIO.sleep(answer.delay).run
        answerHub.publish(answer).run
      }
    }

  private def recordCorrectAnswers(
      correctAnswer: String,
      answers: Dequeue[Answer],
      correctRespondents: Ref[List[Player]]
  ) =
    defer:
      val answer =
        answers.take.run
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

end QuizGame
