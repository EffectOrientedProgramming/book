package Hubs

import fakeEnvironmentInstances.FakeConsole

object HubExploration extends zio.App {
  import zio.*
  import zio.duration.*
  import zio.clock.*
  import zio.console.*

  def run(args: List[String]) = //Use App's run function

//    trait Teacher
    case class Student(name: String)
    case class Question(text: String)
//    case class Survey(questions: List[Question])
    case class Answer(text: String, student: Student)
//    case class Responses(answers: List[Answer])

    def calculatePointsFor(answer: Answer): Int = {
      // TODO Ensure Students only send a single response
      1
    }

//    def calculatePointsFor(answer: List[Answer]): List[(Student, Int)] = ???

    class Scores (studentPoints: Map[Student, Int]):
      def finalResults(): String = ???

    /*
      Teacher --> Survey --> Student1 --> Responses --> Teacher
                             Student2
                             Student3
     */

    val students: List[Student] =
      List(
        Student("Wyett"),
        Student("Bill")
      )

    val cahootSingleRound =
      for
        questionHub <- Hub.bounded[Question](1)
        answerHub <-  Hub.bounded[Answer](students.size)
        _ <- questionHub.subscribe.zip(answerHub.subscribe).use {
          case (questions, answers) => // TODO When do we actually use these subscriptions instead of the outter hub?
            for
              _ <- questionHub.publish(Question("How do you use Hubs?"))
              question <- questions.take
              _ <- answerHub.publish(Answer("Carefully", students.head))
              receivedAnswer <- answers.take
              _ <- putStrLn("Round trip result: " + calculatePointsFor(receivedAnswer))
            yield ()
        }
      yield ()

    val logic =
      for
        hub <- Hub.bounded[Int] (2)
        _ <- hub.subscribe.use {
          case hubSubscription =>

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
              _ <- ZIO.collectAllPar(Set(getAndStoreInput.repeatN(reps), processNextIntAndPrint.forever))
                .timeout(5.seconds)

            yield ()
        }
      yield ()

    (for
      fakeConsole <- FakeConsole.createConsoleWithInput(Seq("3", "5", "7", "9", "11", "13"))
      _ <-
//        logic
        cahootSingleRound
//        .provideCustomLayer(Clock.live ++ ZLayer.succeed(fakeConsole))

    yield ()
      ).exitCode
}
