package Hubs

import fakeEnvironmentInstances.FakeConsole

object HubExploration extends zio.App {
  import zio.*
  import zio.duration.*
  import zio.clock.*
  import zio.console.*

  def run(args: List[String]) = //Use App's run function
    case class Student(name: String)
    case class Question(text: String)
    case class Answer(text: String, student: Student)

    def calculatePointsFor(answer: Answer): Int = {
      // TODO Ensure Students only send a single response
      1
    }

//    def calculatePointsFor(answer: List[Answer]): List[(Student, Int)] = ???

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

    val cahootSingleRound =
      for
        questionHub <- Hub.bounded[Question](1)
        answerHub <- Hub.bounded[Answer](students.size)
        correctAnswers <- Ref.make[Scores](
          Scores(
            students.map((_, 0)).toMap
          )
        )
        _ <- questionHub.subscribe.zip(answerHub.subscribe).use {
          case (
                questions,
                answers
              ) => // TODO When do we actually use these subscriptions instead of the outter hub?
            for
              correctRespondants <- Ref.make[List[Student]](List.empty)
              _ <- questionHub.publish(Question("How do you use Hubs?"))
              question <- questions.take
              _ <- ZIO.collectAllPar(
                Seq(
                  answerHub.publish(Answer("Spain", frop)),
                  for
                    _ <- ZIO.sleep(2.second)
                    _ <- answerHub.publish(Answer("Spain", shtep))
                  yield (),
                  answerHub.publish(Answer("Germany", zeb)),
                  for
                    _ <- ZIO.sleep(1.second)
                    _ <- answerHub.publish(Answer("Spain", cheep))
                  yield ()
                )
              )
              // TODO Add a timeout

              _ <-
                (for // gather answers until there's a winner
                  answer <- answers.take
                  _ <- putStrLn("Response: " + answer)
                  currentCorrectRespondents <- correctRespondants.get
                  _ <-
                    if (answer.text == "Spain")
                      correctRespondants
                        .set(currentCorrectRespondents :+ answer.student)
                    else
                      ZIO.unit
                yield ()).repeat(
                  Schedule
                    .recurUntilM(_ =>
                      correctRespondants.get.map(_.size > 1)
                    ) && Schedule.recurs(3) && Schedule.spaced(1.second)
                )
              winners <- correctRespondants.get
              _ <- putStrLn("Winners: " + winners.mkString(","))
//              _ <- putStrLn(
//                "1st place: " + (firstAnswer, calculatePointsFor(firstAnswer))
//              )
//              _ <- putStrLn(
//                "2nd place: " + (secondAnswer, calculatePointsFor(secondAnswer))
//              )
            yield ()
        }
      yield ()

    val logic =
      for
        hub <- Hub.bounded[Int](2)
        _ <- hub.subscribe.use { case hubSubscription =>
          val getAndStoreInput =
            for
              _ <- console.putStrLn("Please provide an int")
              input <- console.getStrLn
              nextInt = input.toInt
              _ <- hub.publish(nextInt)
            yield ()

          val processNextIntAndPrint =
            for
              nextInt <- hubSubscription.take
              _ <- console.putStrLn("Multiplied Int: " + nextInt * 5)
            yield ()

          val reps = 5
          for
            _ <- ZIO
              .collectAllPar(
                Set(
                  getAndStoreInput.repeatN(reps),
                  processNextIntAndPrint.forever
                )
              )
              .timeout(5.seconds)
          yield ()
        }
      yield ()

    (for
      fakeConsole <- FakeConsole.createConsoleWithInput(
        Seq("3", "5", "7", "9", "11", "13")
      )
      _ <-
//        logic
        cahootSingleRound
//        .provideCustomLayer(Clock.live ++ ZLayer.succeed(fakeConsole))
    yield ()).exitCode
}
