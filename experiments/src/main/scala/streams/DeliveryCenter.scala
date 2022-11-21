package streams

import zio.*
import zio.stream.*

case class Order()

/**
 * Possible stages to demo:
 *  1. Ship individual orders as they come
 *  2. Queue up multiple items and then send
 *  3. Ship partially-filled truck if it has been waiting too long
 */
object DeliveryCenter extends ZIOAppDefault:
  sealed trait Truck

  case class TruckInUse(
      queued: List[Order],
      fuse: Promise[Nothing, Unit],
      capacity: Int = 3
  ) extends Truck:
    val isFull: Boolean =
      queued.length == capacity

  case class TruckEmpty() extends Truck

  def handle(order: Order, staged: Ref[Truck]) =
    def shipIt(reason: String) =
      ZIO.debug(reason + " Ship the orders!") *>
        staged.set(TruckEmpty())

    val loadTruck =
      for
        latch <- Promise.make[Nothing, Unit]
        truck <-
          staged
            .updateAndGet(truck =>
              truck match
                case t: TruckInUse => t.copy(queued = t.queued :+ order)
                case TruckEmpty() =>
                  TruckInUse(List(order), latch)
            )
            .map(_.asInstanceOf[TruckInUse])
        _ <-
          ZIO.debug(
            "Loading order: " +
              truck.queued.length + "/" +
              truck.capacity
          )
      yield truck

    def shipIfWaitingTooLong(truck: TruckInUse) =
      ZIO
        .whenZIO(
          truck
            .fuse
            .isDone
            .map(done => !done)
        )(
          shipIt(reason =
            "Truck has bit sitting half-full too long."
          )
        )
        .delay(4.seconds)

    for
      truck <- loadTruck
      _ <-
        if (truck.isFull)
          shipIt(reason = "Truck is full.")
        else
          ZIO
            .when(truck.queued.length == 1)(
                shipIfWaitingTooLong(truck)
            )
            .forkDaemon
    yield ()
    end for
  end handle

  def run =
    for
      stagedItems <-
        Ref.make[Truck](TruckEmpty())
      orderStream =
        ZStream.repeatWithSchedule(
          Order(),
          Schedule
            .exponential(1.second, factor = 1.8)
        )
      _ <-
        orderStream
          .foreach(handle(_, stagedItems))
          .timeout(12.seconds)
    yield ()
end DeliveryCenter
