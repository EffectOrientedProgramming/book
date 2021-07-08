package Hubs

import fakeEnvironmentInstances.FakeConsole

import java.io.IOException

object HubExploration extends zio.App {
  import zio.*
  import zio.duration.*
  import zio.clock.*
  import zio.console.*

  def run(args: List[String]) = //Use App's run function
    case class Student(name: String)
    case class Question(text: String, correctResponse: String)
    case class Answer(student: Student, text: String, delay: Duration)
    case class RoundDescription(question: Question, answers: Seq[Answer])

    def calculatePointsFor(answer: Answer): Int = {
      // TODO Ensure Students only send a single response
      1
    }

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

    def processAnswers(
        correctAnswer: String,
        answers: ZDequeue[Any, Nothing, Answer],
        correctRespondants: Ref[List[Student]]
    ) =
      for // gather answers until there's a winner
        answer <- answers.take
        _ <- putStrLn("Response: " + answer)
        currentCorrectRespondents <- correctRespondants.get
        _ <-
          if (answer.text == correctAnswer)
            for
              _ <- putStrLn("Correct response from: " + answer.student)
              _ <- correctRespondants
                .set(currentCorrectRespondents :+ answer.student)
            yield ()
          else
            ZIO.unit
      yield ()

    def untilWinnersAreFound(correctRespondants: Ref[List[Student]]) =
      Schedule
        .recurUntilM(_ => correctRespondants.get.map(_.size > 1))

    val round1Responses = Seq(
      Answer(frop, "Spain", 1.seconds),
      Answer(zeb, "Germany", 1.seconds),
      Answer(cheep, "Spain", 2.seconds),
      Answer(shtep, "Spain", 3.seconds)
    )

    val round1 =
      RoundDescription(
        Question("What is the southern-most European country?", "Spain"),
        round1Responses
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

            // TODO Get types of these 2 operations to sync up, so they can be run in parallel
            val answerProcessingAndReporting =
              for
                successfulCompletion <-
                  processAnswers("Spain", answers, correctRespondants)
                    .repeat(
                      untilWinnersAreFound(correctRespondants)
                    )
                    .timeout(
                      2.second
                    )
                winners <- correctRespondants.get
                _ <- successfulCompletion match {
                  case Some(_) => putStrLn("Winners: " + winners.mkString(","))
                  case None =>
                    putStrLn(
                      "Winners of incomplete round: " + winners.mkString(",")
                    )
                }
              yield ()
            val submitAnswers =
              ZIO
                .collectAllPar(
                  round1.answers.map { case answer =>
                    for
                      _ <- ZIO.sleep(answer.delay)
                      _ <- answerHub.publish(answer)
                    yield ()
                  }
                )
            for
              _ <- questionHub.publish(
                round1.question
              )
              question <- questions.take
              _ <- submitAnswers
              // TODO This next part should happen *simultaneously* with the contestants answering.
              _ <- answerProcessingAndReporting
            yield ()
          }
        }
      yield ()

    cahootSingleRound.exitCode
}
