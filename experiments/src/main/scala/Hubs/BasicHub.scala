package Hubs

import zio.*
import zio.direct.*
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
              defer {
                Hub.publish(
                  "Hub message"
                ).run

                val leftItem = left
                  .take
                  .run

                Console.printLine("Left item: " + leftItem).run

                val rightItem = right
                  .take
                  .run

                Console.printLine("Right item: " + rightItem).run
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
