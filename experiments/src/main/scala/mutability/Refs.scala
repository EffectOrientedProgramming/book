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
    (for
      results <-
        ZIO.foreachParDiscard(Range(0, 10000))(
          _ => incrementCounter()
        ).timed
      _ <- ZIO.debug("Final count: " + counter)
      _ <- ZIO.debug("Duration: " + results._1.toMillis)
    yield ())


object ReliableMutability
  extends ZIOAppDefault:
  def incrementCounter(counter: Ref[Int]) =
    counter.update(_ + 1)

  def run =
    for
      counter <- Ref.make(0)
      results <-
        ZIO.foreachParDiscard(Range(0, 10000))(
          _ => incrementCounter(counter)
        ).timed
      finalResult <- counter.get
      _ <- ZIO.debug("Final count: " + finalResult)
      _ <- ZIO.debug("Duration: " + results._1.toMillis)
    yield ()


object MutabilityWithComplexTypes
  extends ZIOAppDefault:


  case class Sensor():
    def read: ZIO[Any, Nothing, SensorData] = zio.Random.nextIntBounded(10).map(SensorData(_))

  case class SensorData(value: Int)

  case class World(sensors: List[Sensor], currentData: Ref[List[SensorData]])

  val arbitrarilyChangeWorldData =
    for
      currentData: Ref[List[SensorData]] <- Ref.make(List.fill(100)(0).map(SensorData(_)))
      world: World = World(List.fill(100)(Sensor()), currentData)
      _ <- world.currentData.get.debug("Current data: ")
      _ <- world.currentData.update(oldData=>
        oldData.zipWithIndex.map{case (_, idx) =>
          if (idx < 10)
            SensorData(5)
          else
            SensorData(0)
        }
      )
      _ <- world.currentData.get.debug("Updated data: ")
    yield ()

  def run =

    readFromSensors

  val readFromSensors =
    for
      currentData: Ref[List[SensorData]] <- Ref.make(List.fill(100)(0).map(SensorData(_)))
      world: World = World(List.fill(100)(Sensor()), currentData)
      _ <- world.currentData.get.debug("Current data: ")
      updatedSensorReadings <- ZIO.foreach(world.sensors)(_.read)
      _ <- world.currentData.set(updatedSensorReadings)
      _ <- world.currentData.get.debug("Updated data from sensors: ")
    yield ()




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
