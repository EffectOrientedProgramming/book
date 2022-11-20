package streams

import zio.*
import zio.stream.*

case class Order()

object DeliveryCenter extends ZIOAppDefault:
  def handle(order: Order, staged: Ref[List[Order]]) =
    val truckCapacity = 3
    for
      orders <- staged.updateAndGet(_ :+ order)
      _ <-
        if (orders.length == truckCapacity)
          ZIO
            .debug("Ship the orders!") *>
            staged.set(List.empty)
        else
          ZIO.debug("Queuing order: " + (orders.length) + "/" + truckCapacity) *>
            ZIO.when(orders.isEmpty)(
                ZIO.whenZIO(staged.get.map(_.nonEmpty))(
                  ZIO.debug("Truck has bit sitting half-full too long. Send it!") *>
                    staged.set(List.empty)
                ).delay(2.seconds)
            ).forkDaemon
    yield ()

  def run =
    (
      for
        stagedItems <-
          Ref.make(List.empty[Order])
        orderStream =
          ZStream.repeatWithSchedule(
            Order(),
            Schedule.exponential(1.second)
          )
        _ <-
          orderStream.foreach(handle(_, stagedItems)
          )
      yield ()
    ).timeout(15.seconds)
end DeliveryCenter
