package Hubs

import console.FakeConsole
import zio.ZIO
import zio.*
import zio.Duration.*
import zio.Clock.*
import zio.Console.*
import zio.direct.*

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
