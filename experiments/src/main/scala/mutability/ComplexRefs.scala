package mutability

import zio.*
import zio.direct.*

object ComplexRefs extends ZIOAppDefault:

  class Sensor(lastReading: Ref[SensorData]):
    def read: ZIO[Any, Nothing, SensorData] =
      zio
        .Random
        .nextIntBounded(10)
        .map(SensorData(_))

  object Sensor:
    val make: ZIO[Any, Nothing, Sensor] =
      for lastReading <- Ref.make(SensorData(0))
      yield Sensor(lastReading)

  case class SensorData(value: Int)

  case class World(sensors: List[Sensor])

  val readFromSensors =
    defer {
      val sensors =
        ZIO.foreach(List.fill(100)(0))(_ =>
          Sensor.make
        ).run
      val world = World(sensors)
      ZIO
        .foreach(world.sensors)(_.read)
        .debug("Current data: ")
        .run
    }

  def run = readFromSensors

end ComplexRefs
