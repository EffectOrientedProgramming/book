package mutability

import mutability.UnreliableMutability.incrementCounter
import zio.{Ref, ZIO, ZIOAppDefault}

object UnreliableMutability
    extends ZIOAppDefault:
  var counter = 0
  def incrementCounter() =
    ZIO.succeed {
      counter = counter + 1
      counter
    }

  def run =
    for
      results <-
        ZIO
          .foreachParDiscard(Range(0, 10000))(
            _ => incrementCounter()
          )
          .timed
      _ <- ZIO.debug("Final count: " + counter)
      _ <-
        ZIO.debug(
          "Duration: " + results._1.toMillis
        )
    yield ()
end UnreliableMutability

object ReliableMutability extends ZIOAppDefault:
  def incrementCounter(counter: Ref[Int]) =
    counter.update(_ + 1)

  def run =
    for
      counter <- Ref.make(0)
      results <-
        ZIO
          .foreachParDiscard(Range(0, 10000))(
            _ => incrementCounter(counter)
          )
          .timed
      finalResult <- counter.get
      _ <-
        ZIO.debug("Final count: " + finalResult)
      _ <-
        ZIO.debug(
          "Duration: " + results._1.toMillis
        )
    yield ()
end ReliableMutability

object MutabilityWithComplexTypes
    extends ZIOAppDefault:

  class Sensor(lastReading: Ref[SensorData]):
    def read: ZIO[Any, Nothing, SensorData] =
      zio
        .Random
        .nextIntBounded(10)
        .map(SensorData(_))

  object Sensor:
    val make: ZIO[Any, Nothing, Sensor] =
      for lastReading <- Ref.make(SensorData(0))
      yield new Sensor(
        lastReading
      ) // TODO Why do we need new?

  case class SensorData(value: Int)

  case class World(sensors: List[Sensor])

  val arbitrarilyChangeWorldData =
    for
      sensors <-
        ZIO.foreach(List.fill(100)(0))(_ =>
          Sensor.make
        )
      world = World(sensors)
      _ <-
        ZIO
          .foreach(world.sensors)(_.read)
          .debug("Current data: ")
    yield ()

  def run = arbitrarilyChangeWorldData
//    readFromSensors

  val readFromSensors =
    for _ <- ZIO.unit
//      currentData <- Ref.make(List.fill(100)(SensorData(0)))
//      sensors = List.fill(100)(Sensor())
//      world = World(sensors, currentData)
//      _ <- world.currentData.get.debug("Current data: ")
//      updatedSensorReadings <- ZIO.foreach(world.sensors)(_.read)
//      _ <- world.currentData.set(updatedSensorReadings)
//      _ <- world.currentData.get.debug("Updated data from sensors: ")
    yield ()
end MutabilityWithComplexTypes

object Refs extends ZIOAppDefault:
  def run =
    for
      ref        <- Ref.make(1)
      startValue <- ref.get
      _ <-
        ZIO.debug("start value: " + startValue)
      _          <- ref.set(5)
      finalValue <- ref.get
      _ <-
        ZIO.debug("final value: " + finalValue)
    yield ()
