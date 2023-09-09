# Location

Consider the term `Environment`.
In common speech, this often indicates _where_ something happens.
Previously, we have examined this in terms of "Which Machine?"

However, it is equally valid to treat this as a spatial location at which our code is executed.

```scala mdoc
trait HardwareFailure
case class GpsCoordinates(
    latitude: Double,
    longitude: Double
)

trait TimeZone

trait Location:
  def gpsCoords
      : ZIO[Any, HardwareFailure, GpsCoordinates]
  def timezone: ZIO[Any, Nothing, TimeZone]

object Location:
  def gpsCoords: ZIO[
    Location,
    HardwareFailure,
    GpsCoordinates
  ] = ZIO.service[Location].flatMap(_.gpsCoords)
```

Now that we have basic `Location`-awareness, we can build more domain-specific logic on top of it.


```scala mdoc
trait FloodStatus
object Safe       extends FloodStatus
object Threatened extends FloodStatus

trait FloodWarning:
  def seaLevelStatus
      : ZIO[Any, Nothing, FloodStatus]
```

```scala mdoc
case class Slope(degrees: Float)

trait Topography:
  def slope: ZIO[Location, Nothing, Slope]
```

```scala mdoc
case class Rainfall(inches: Int)

trait Almanac:
  def averageAnnualRainfail
      : ZIO[Location, Nothing, Rainfall]
```


```scala mdoc
case class Country(name: String)

trait CountryService:
  def currentCountry
      : ZIO[Location, HardwareFailure, Country]

object CountryService:
  // TODO investigate bad formatting
  def currentCountry
      : ZIO[Location, HardwareFailure, Country] =
    defer {
      val gpsCords = Location.gpsCoords.run
      if (gpsCords.latitude > 0)
        Country("Canada")
      else
        Country("USA")
    }
```

```scala mdoc
trait LegalStatus
object Legal   extends LegalStatus
object Illegal extends LegalStatus

trait GeoPolitcalState
trait CurrentWar

enum Issue:
  case OnlineGambling,
    Alcohol

trait LawLibrary:
  def status(
      country: Country,
      issue: Issue
  ): ZIO[
    GeoPolitcalState,
    CurrentWar,
    LegalStatus
  ]

class LegalService(
    countryService: CountryService,
    lawLibrary: LawLibrary
):
  def status(issue: Issue): ZIO[
    Location & GeoPolitcalState,
    CurrentWar | HardwareFailure,
    LegalStatus
  ] =
    defer {
      val country =
        countryService.currentCountry.run
      lawLibrary.status(country, issue).run
    }
```
