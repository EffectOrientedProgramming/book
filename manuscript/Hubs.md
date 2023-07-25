## Hubs

 

### experiments/src/main/scala/Hubs/BasicHub.scala
```scala
package Hubs

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
              defer {
                Hub.publish("Hub message").run

                val leftItem = left.take.run

                Console
                  .printLine(
                    "Left item: " + leftItem
                  )
                  .run

                val rightItem = right.take.run

                Console
                  .printLine(
                    "Right item: " + rightItem
                  )
                  .run
              }
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

```


### experiments/src/main/scala/Hubs/ReadIntAndMultiply.scala
```scala
package Hubs

import console.FakeConsole
import zio.Console.*

object ReadIntAndMultiply
    extends zio.ZIOAppDefault:

  def run = // Use App's run function
    val logic =
      defer {
        val hub = Hub.bounded[Int](2).run
        ZIO
          .scoped {
            defer {
              val hubSubscription =
                hub.subscribe.run
              val getAndStoreInput =
                defer {
                  Console
                    .printLine(
                      "Please provide an int"
                    )
                    .run
                  val input =
                    Console.readLine.run
                  val nextInt = input.toInt
                  hub.publish(nextInt).run
                }

              val processNextIntAndPrint =
                defer {
                  val nextInt =
                    hubSubscription.take.run
                  Console
                    .printLine(
                      "Multiplied Int: " +
                        nextInt * 5
                    )
                    .run
                }

              val reps = 5
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
                .run
            }
          }
          .run
      }

    defer {
      val fakeConsole =
        FakeConsole
          .withInput(
            "3",
            "5",
            "7",
            "9",
            "11",
            "13"
          )
          .run
      logic.withConsole(fakeConsole).run
    }
  end run
end ReadIntAndMultiply

```


