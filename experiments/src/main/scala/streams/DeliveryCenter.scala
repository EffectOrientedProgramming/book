package streams

import zio.*
import zio.stream.*

case class Order()

object DeliveryCenter extends ZIOAppDefault:
  sealed trait Truck
  case class TruckInUse(
      queued: List[Order],
      fuse: Promise[Nothing, Unit]
  ) extends Truck
  case class TruckEmpty() extends Truck
  def handle(order: Order, staged: Ref[Truck]) =
    val shipIt =
      ZIO.debug("Ship the orders!") *>
        staged.set(TruckEmpty())

    val truckCapacity = 3
    for
      // TODO Only make this down below
      latch <- Promise.make[Nothing, Unit]
      truckZ <-
        staged.updateAndGet(truck =>
          truck match
            case TruckInUse(queued, fuse) =>
              TruckInUse(queued :+ order, fuse)
            case TruckEmpty() =>
              TruckInUse(List(order), latch)
        )
      truck =
        truckZ match
          case t: TruckInUse =>
            t
          case TruckEmpty() =>
            ???
      _ <-
        ZIO.debug(
          "Queuing order: " +
            (truck.queued.length) + "/" +
            truckCapacity
        )
      _ <-
        if (truck.queued.length == truckCapacity)
          shipIt *> truck.fuse.succeed(())
        else
          ZIO
            .when(truck.queued.length == 1)(
              ZIO
                .whenZIO(
                  latch.isDone.map(done => !done)
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
