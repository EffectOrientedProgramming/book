package streams

import zio.*
import zio.stream.*

case class Order()

object DeliveryCenter extends ZIOAppDefault:
  def handle(order: Order, staged: Ref[List[Order]]) =
    val shipIt =
      ZIO
        .debug("Ship the orders!") *>
        staged.set(List.empty)

    val truckCapacity = 3
    for
      orders <- staged.updateAndGet(_ :+ order)
      _ <-
        if (orders.length == truckCapacity)
          ZIO.debug("Truck is full.") *>
            shipIt
        else
          ZIO.debug("Queuing order: " + (orders.length) + "/" + truckCapacity) *>
            ZIO.when(orders.length == 1)(
                ZIO.debug("Setting timeout") *>
                ZIO.whenZIO(staged.get.map(_.nonEmpty))(
                  ZIO.debug("Truck has bit sitting half-full too long.") *> shipIt
                ).delay(5.seconds)
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
