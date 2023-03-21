## Hubs

 

### experiments/src/main/scala/Hubs/BasicHub.scala
```scala
package Hubs

import zio.*
import zio.Duration
import zio.Clock
import zio.Console

// The purpose of this example to to create a
// very basic hub that displays small
// capabilities.

object BasicHub extends zio.ZIOAppDefault:

  // This example makes a hub, and publishes a
  // String. Then, two entities take the
  // published string and print it.
  val logic1 =
    Hub
      .bounded[String](2)
      .flatMap { Hub =>
        ZIO.scoped {
          Hub
            .subscribe
            .zip(Hub.subscribe)
            .flatMap { case (left, right) =>
              for
                _ <-
                  Hub.publish(
                    "This is from Hub left!"
                  )
                _ <-
                  left
                    .take
                    .flatMap(
                      Console.printLine(_)
                    )
                _ <-
                  right
                    .take
                    .flatMap(
                      Console.printLine(_)
                    )
              yield ()
            }
        }
      }

  /* case class entity(name:String) case class
   * question(ques:String) case class
   * response(rep:String, ent:entity) val
   * entities = List(entity("Bob"),
   * entity("Smith")) //This example sends out a
   * question in the form of a string. Then, two
   * //entities respond with different reponses.
   * val logic2 =
   * for questHub <- Hub.bounded[question](1)
   * repHub <-
   * Hub.bounded[response](entities.size) _ <-
   * questHub.subscribe.zip(repHub.subscribe).use
   * { case ( Quest, Resp ) =
   *
   * } */

  def run = logic1.exitCode
end BasicHub

```


### experiments/src/main/scala/Hubs/QuizGame.scala
```scala
package Hubs

import console.FakeConsole

import java.io.IOException
import zio.{
  Clock,
  Console,
  Dequeue,
  Duration,
  Hub,
  Ref,
  Schedule,
  ZIO,
  durationInt
}
import zio.Console.printLine

object QuizGame extends zio.ZIOAppDefault:
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

  def run = // Use App's run function

    /* Teacher --> Questions --> Student1 -->
     * Answers --> Teacher Student2 Student3 */

    val frop  = Player("Frop")
    val zeb   = Player("Zeb")
    val shtep = Player("Shtep")
    val cheep = Player("Cheep")

    val students: List[Player] =
      List(frop, zeb, shtep, cheep)

    def submitAnswersAfterDelay(
        answerHub: Hub[Answer],
        answers: Seq[Answer]
    ) =
      ZIO
        .collectAllPar(
          answers.map { answer =>
            for
              _ <- ZIO.sleep(answer.delay)
              _ <- answerHub.publish(answer)
            yield ()
          }
        )
        .unit

    def recordCorrectAnswers(
        correctAnswer: String,
        answers: Dequeue[Answer],
        correctRespondents: Ref[List[Player]]
    ) =
      for // gather answers until there's a winner
        answer <- answers.take
        output <-
          if (answer.text == correctAnswer)
            for
              currentCorrectRespondents <-
                correctRespondents.get
              _ <-
                correctRespondents.set(
                  currentCorrectRespondents :+
                    answer.player
                )
            yield "Correct response from: " +
              answer.player
          else
            ZIO.succeed(
              "Incorrect response from: " +
                answer.player
            )
        _ <- printLine(output)
      yield ()

    def untilWinnersAreFound(
        correctRespondents: Ref[List[Player]]
    ) =
      Schedule.recurUntilZIO(_ =>
        correctRespondents.get.map(_.size == 2)
      )

    def printRoundResults(
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
        roundWithMultipleCorrectAnswers,
        roundWithOnly1CorrectAnswer,
        roundWhereEverybodyIsWrong
      )

    val cahootSingleRound =
      for
        questionHub <- Hub.bounded[Question](1)
        answerHub: Hub[Answer] <-
          Hub.bounded[Answer](students.size)
        correctRespondents: Ref[List[Player]] <-
          Ref.make[List[Player]](List.empty)
        _ <-
          questionHub
            .subscribe
            .zip(answerHub.subscribe)
            .flatMap {
              case (
                    questions,
                    answers: Dequeue[Answer]
                  ) =>
                def playARound(
                    roundDescription: RoundDescription
                ) =
                  for
                    _ <-
                      printLine(
                        "==============================="
                      )
                    _ <-
                      printLine(
                        "Question for round: " +
                          roundDescription
                            .question
                            .text
                      )
                    _ <-
                      correctRespondents
                        .set(List.empty)
                    _ <-
                      questionHub.publish(
                        roundDescription.question
                      )
                    question <- questions.take
                    _ <-
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
                    winners <-
                      correctRespondents.get
                    _ <-
                      printRoundResults(winners)
                    _ <-
                      printLine(
                        "==============================="
                      )
                  yield ()

                ZIO.foreach(rounds)(playARound)
            }
      yield ()

    cahootSingleRound.exitCode
  end run
end QuizGame

```


### experiments/src/main/scala/Hubs/ReadIntAndMultiply.scala
```scala
package Hubs

import console.FakeConsole
import zio.ZIO
import zio.*
import zio.Duration.*
import zio.Clock.*
import zio.Console.*

object ReadIntAndMultiply
    extends zio.ZIOAppDefault:

  def run = // Use App's run function
    val logic =
      for
        hub <- Hub.bounded[Int](2)
        _ <-
          ZIO.scoped {
            hub
              .subscribe
              .flatMap { hubSubscription =>
                val getAndStoreInput =
                  for
                    _ <-
                      Console.printLine(
                        "Please provide an int"
                      )
                    input <- Console.readLine
                    nextInt = input.toInt
                    _ <- hub.publish(nextInt)
                  yield ()

                val processNextIntAndPrint =
                  for
                    nextInt <-
                      hubSubscription.take
                    _ <-
                      Console.printLine(
                        "Multiplied Int: " +
                          nextInt * 5
                      )
                  yield ()

                val reps = 5
                for _ <-
                    ZIO
                      .collectAllPar(
                        Set(
                          getAndStoreInput
                            .repeatN(reps),
                          processNextIntAndPrint
                            .forever
                        )
                      )
                      .timeout(5.seconds)
                yield ()
              }
          }
      yield ()

    (
      for
        fakeConsole <-
          FakeConsole.withInput(
            "3",
            "5",
            "7",
            "9",
            "11",
            "13"
          )
        _ <- logic.withConsole(fakeConsole)
      yield ()
    ).exitCode
  end run
end ReadIntAndMultiply

```


