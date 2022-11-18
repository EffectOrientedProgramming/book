package streams

import zio.*
import zio.stream.*

case class Order()

object DeliveryCenter extends ZIOAppDefault:
  def run =
    (for
      stagedItems <- Ref.make(List.empty[Order])
      orderStream = ZStream.repeatWithSchedule(Order(), Schedule.exponential(1.second))
      _ <- orderStream.foreach( order =>
        for
          orders <- stagedItems.get
          _ <-
            if (orders.length == 2)
              ZIO.debug("Ship the orders!") *> stagedItems.set(List.empty)
            else
              ZIO.debug("Queuing order") *> stagedItems.update(_ :+ order)
        yield ()
      )
//      _ <- orderStream.runDrain
    yield ()).timeout(10.seconds)

