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

  def run = logic1.exitCode
end BasicHub
