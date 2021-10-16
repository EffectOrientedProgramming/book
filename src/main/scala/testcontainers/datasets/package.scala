package testcontainers

package object datasets {}

object ServiceDataSets:
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

  val careerData: ExpectedData =
    List(
      RequestResponsePair("/Joe", "Job:Athlete"),
      RequestResponsePair(
        "/Shtep",
        "Job:Salesman"
      ),
      RequestResponsePair("/Zeb", "Job:Mechanic")
    )

  val locations: ExpectedData =
    List(
      RequestResponsePair("/Joe", "USA"),
      RequestResponsePair("/Shtep", "Jordan"),
      RequestResponsePair("/Zeb", "Taiwan")
    )

  val backgroundData: ExpectedData =
    List(
      RequestResponsePair("/Joe", "GoodCitizen"),
      RequestResponsePair(
        "/Shtep",
        "Arson,DomesticViolence"
      ),
      RequestResponsePair(
        "/Zeb",
        "SpeedingTicket"
      )
    )
end ServiceDataSets
