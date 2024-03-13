package streams

import zio.stream.*

case class Order()

/** Possible stages to demo:
  *   1. Ship individual orders as they come 2.
  *      Queue up multiple items and then send 3.
  *      Ship partially-filled truck if it has
  *      been waiting too long
  */
object DeliveryCenter extends ZIOAppDefault:
  sealed trait Truck

  case class TruckInUse(
      queued: List[Order],
      fuse: Promise[Nothing, Unit],
      capacity: Int =
        3
  ) extends Truck:
    val isFull: Boolean =
      queued.length == capacity

    val waitingTooLong =
      fuse
        .isDone
        .map(
          done => !done
        )

  def handle(
      order: Order,
      staged: Ref[Option[TruckInUse]]
  ) =
    def shipIt(reason: String) =
      defer:
        ZIO
          .debug(reason + " Ship the orders!")
          .run
        staged
          .get
          .flatMap(_.get.fuse.succeed(()))
          .run
        staged.set(None).run

    val loadTruck =
      defer {
        val latch =
          Promise.make[Nothing, Unit].run
        val truck =
          staged
            .updateAndGet(
              truck =>
                truck match
                  case Some(t) =>
                    Some(
                      t.copy(queued =
                        t.queued :+ order
                      )
                    )
                  case None =>
                    Some(
                      TruckInUse(
                        List(order),
                        latch
                      )
                    )
            )
            .map(_.get)
            .run
        ZIO
          .debug(
            "Loading order: " +
              truck.queued.length + "/" +
              truck.capacity
          )
          .run
        truck
      }

    def shipIfWaitingTooLong(truck: TruckInUse) =
      ZIO
        .whenZIO(truck.waitingTooLong)(
          shipIt(reason =
            "Truck has bit sitting half-full too long."
          )
        )
        .delay(4.seconds)

    defer {
      val truck =
        loadTruck.run
      if (truck.isFull)
        shipIt(reason =
          "Truck is full."
        ).run
      else
        ZIO
          .when(truck.queued.length == 1)(
            ZIO.debug("Adding timeout daemon") *>
              shipIfWaitingTooLong(truck)
          )
          .forkDaemon
          .run
    }
  end handle

  def run =
    defer {
      val stagedItems =
        Ref.make[Option[TruckInUse]](None).run

      val orderStream =
        ZStream.repeatWithSchedule(
          Order(),
          Schedule.exponential(
            1.second,
            factor =
              1.8
          )
        )
      orderStream
        .foreach(handle(_, stagedItems))
        .timeout(12.seconds)
        .run
    }
end DeliveryCenter
