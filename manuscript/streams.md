## streams

 

### experiments/src/main/scala/streams/DeliveryCenter.scala
```scala
package streams

import zio.*
import zio.stream.*

case class Order()

object DeliveryCenter extends ZIOAppDefault:
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
          orderStream.foreach(order =>
            for
              orders <- stagedItems.get
              _ <-
                if (orders.length == 2)
                  ZIO
                    .debug("Ship the orders!") *>
                    stagedItems.set(List.empty)
                else
                  ZIO.debug("Queuing order") *>
                    stagedItems
                      .update(_ :+ order)
            yield ()
          )
//      _ <- orderStream.runDrain
      yield ()
    ).timeout(10.seconds)
end DeliveryCenter

```


### experiments/src/main/scala/streams/HelloStreams.scala
```scala
package streams

import zio.*
import zio.stream.*

object HelloStreams extends ZIOAppDefault:
  def run =
    for
      _ <- ZIO.debug("Stream stuff!")
      greetingStream =
        ZStream.repeatWithSchedule(
          "Hi",
          Schedule.spaced(1.seconds)
        )
      insultStream =
        ZStream.repeatWithSchedule(
          "Dummy",
          Schedule.spaced(2.seconds)
        )
      combinedStream =
        ZStream.mergeAllUnbounded()(
          greetingStream,
          insultStream
        )
      aFewElements = combinedStream.take(6)
      res <- aFewElements.runCollect
      _   <- ZIO.debug("Res: " + res)
    yield ()
end HelloStreams

```

