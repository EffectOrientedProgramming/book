package testcontainers

import zio.{Has, Layer, ZLayer}

package object datasets {}

object ServiceDataSets:
  case class CareerData(
      expectedData: ExpectedData
  )
  case class BackgroundData(
      expectedData: ExpectedData
  )
  case class LocationData(
      expectedData: ExpectedData
  )

  opaque type ExpectedData =
    List[RequestResponsePair]

  extension (expectedData: ExpectedData)
    def foreach[U](
        f: RequestResponsePair => U
    ): Unit = expectedData.foreach(f)

    def find(
        p: RequestResponsePair => Boolean
    ): Option[RequestResponsePair] =
      expectedData.find(p)

  // TODO remove
  val careerData: CareerData =
    CareerData(
      List(
        RequestResponsePair(
          "/Joe",
          "Job:Athlete"
        ),
        RequestResponsePair(
          "/Shtep",
          "Job:Salesman"
        ),
        RequestResponsePair(
          "/Zeb",
          "Job:Mechanic"
        )
      )
    )

  val careerDataZ
      : Layer[Nothing, Has[CareerData]] =
    ZLayer.succeed(careerData)

  val locations
      : Layer[Nothing, Has[LocationData]] =
    ZLayer.succeed(
      LocationData(
        List(
          RequestResponsePair("/Joe", "USA"),
          RequestResponsePair(
            "/Shtep",
            "Jordan"
          ),
          RequestResponsePair("/Zeb", "Taiwan")
        )
      )
    )

  val backgroundData: BackgroundData =
    BackgroundData(
      List(
        RequestResponsePair(
          "/Joe",
          "GoodCitizen"
        ),
        RequestResponsePair(
          "/Shtep",
          "Arson,DomesticViolence"
        ),
        RequestResponsePair(
          "/Zeb",
          "SpeedingTicket"
        )
      )
    )
end ServiceDataSets
