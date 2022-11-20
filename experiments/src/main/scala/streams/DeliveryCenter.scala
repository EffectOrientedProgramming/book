package streams

import zio.*
import zio.stream.*

case class Order()

object DeliveryCenter extends ZIOAppDefault:
  sealed trait Truck
  case class TruckInUse(
      queued: List[Order],
      fuse: Promise[Nothing, Unit],
      capacity: Int = 3
  ) extends Truck
  case class TruckEmpty() extends Truck
  def handle(order: Order, staged: Ref[Truck]) =
    val shipIt =
      ZIO.debug("Ship the orders!") *>
        staged.set(TruckEmpty())

    val loadTruck =
      for
        latch <- Promise.make[Nothing, Unit]
        truck <-
          staged.updateAndGet(truck =>
            truck match
              case TruckInUse(queued, fuse, capacity) =>
                TruckInUse(queued :+ order, fuse, capacity)
              case TruckEmpty() =>
                TruckInUse(List(order), latch)
          ).map(_.asInstanceOf[TruckInUse])
        _ <-
          ZIO.debug(
            "Loading order: " +
              (truck.queued.length) + "/" +
              truck.capacity
          )
      yield truck

    for
      truck <- loadTruck
      _ <-
        if (truck.queued.length == truck.capacity)
          shipIt *> truck.fuse.succeed(())
        else
          ZIO
            .when(truck.queued.length == 1)(
              ZIO
                .whenZIO(
                  truck.fuse.isDone.map(done => !done)
                )(
                  ZIO.debug(
                    "Truck has bit sitting half-full too long."
                  ) *> shipIt
                )
                .delay(4.seconds)
            )
            .forkDaemon
    yield ()
    end for
  end handle

  def run =
    (
      for
        stagedItems <-
          Ref.make[Truck](TruckEmpty())
        orderStream =
          ZStream.repeatWithSchedule(
            Order(),
            Schedule.exponential(
              1.second,
              factor = 1.8
            )
          )
        _ <-
          orderStream
            .foreach(handle(_, stagedItems))
      yield ()
    ).timeout(12.seconds)
end DeliveryCenter
