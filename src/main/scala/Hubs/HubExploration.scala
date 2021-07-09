package Hubs

import fakeEnvironmentInstances.FakeConsole

import java.io.IOException

import zio.{Hub, Ref, Schedule, ZDequeue, ZIO}
import zio.duration.{Duration, durationInt}
import zio.clock.Clock
import zio.console.{Console, putStrLn}

object HubExploration extends zio.App {
  case class Student(name: String)
  case class Question(text: String, correctResponse: String)
  case class Answer(student: Student, text: String, delay: Duration)
  case class RoundDescription(question: Question, answers: Seq[Answer])

  def run(args: List[String]) = //Use App's run function

    class Scores(studentPoints: Map[Student, Int]):
      def finalResults(): String = ???

    /*
      Teacher --> Survey --> Student1 --> Responses --> Teacher
                             Student2
                             Student3
     */

    val frop = Student("Frop")
    val zeb = Student("Zeb")
    val shtep = Student("Shtep")
    val cheep = Student("Cheep")

    val students: List[Student] =
      List(
        frop,
        zeb,
        shtep,
        cheep
      )

    def answerProcessingAndReporting(
        answers: ZDequeue[Any, Nothing, Answer],
        correctRespondants: Ref[List[Student]]
    ) =
      for
        successfulCompletion <-
          recordCorrectAnswers("Spain", answers, correctRespondants)
            .repeat(
              untilWinnersAreFound(correctRespondants)
            )
            .timeout(
              4.second
            )
        winners <- correctRespondants.get
        finalOutput = successfulCompletion match {
          case Some(_) => "Winners: " + winners.mkString(",")
          case None =>
            if (winners.isEmpty)
              "Nobody submitted a correct response"
            else
              "Winners of incomplete round: " + winners.mkString(",")
        }
        _ <- putStrLn(finalOutput)
      yield ()

    def recordCorrectAnswers(
        correctAnswer: String,
        answers: ZDequeue[Any, Nothing, Answer],
        correctRespondants: Ref[List[Student]]
    ) =
      for // gather answers until there's a winner
        answer <- answers.take
        _ <-
          if (answer.text == correctAnswer)
            for
              _ <- putStrLn("Correct response from: " + answer.student)
              currentCorrectRespondents <- correctRespondants.get
              _ <- correctRespondants
                .set(currentCorrectRespondents :+ answer.student)
            yield ()
          else
            putStrLn("Incorrect response from: " + answer.student)
      yield ()

    def untilWinnersAreFound(correctRespondants: Ref[List[Student]]) =
      Schedule
        .recurUntilM(_ => correctRespondants.get.map(_.size == 2))

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

    val cahootSingleRound =
      for
        questionHub <- Hub.bounded[Question](1)
        answerHub: Hub[Answer] <- Hub.bounded[Answer](students.size)
        correctAnswers <- Ref.make[Scores](
          Scores(
            students.map((_, 0)).toMap
          )
        )
        correctRespondants: Ref[List[Student]] <- Ref
          .make[List[Student]](List.empty)
        _ <- questionHub.subscribe.zip(answerHub.subscribe).use {
          case (
                questions,
                answers: ZDequeue[Any, Nothing, Answer]
              ) => { // TODO When do we actually use these subscriptions instead of the outter hub?

            val submitAnswersAfterDelay =
              ZIO
                .collectAllPar(
                  round1.answers.map { case answer =>
                    for
                      _ <- ZIO.sleep(answer.delay)
                      _ <- answerHub.publish(answer)
                    yield ()
                  }
                )
                .map(_ => ())

            for
              _ <- questionHub.publish(
                round1.question
              )
              question <- questions.take
              _ <- ZIO
                .collectAllPar( // TODO timeout for this composed piece, so submitAnswersAfterDelay can't block
                  Seq(
                    submitAnswersAfterDelay,
                    answerProcessingAndReporting(answers, correctRespondants)
                  )
                )
            yield ()
          }
        }
      yield ()

    cahootSingleRound.exitCode
}
