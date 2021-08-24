# Location

Consider the term `Environment`.
In common speech, this often indicates _where_ something happens.
Previously, we have examined this in terms of "Which Machine?"

However, it is equally valid to treat this as a spatial location at which our code is executed.

TODO {{Break this down into bite-sized pieces}}

```scala mdoc
import zio.ZIO
```

```scala mdoc
trait HardwareFailure
case class GpsCoordinates(latitude: Double, longitude: Double)

trait TimeZone

trait Country

trait SeaLevelStatus
object Above extends SeaLevelStatus
object Below extends SeaLevelStatus

trait Location:
  def gpsCoords: ZIO[Any, HardwareFailure, GpsCoordinates]
  def timezone: ZIO[Any, Nothing, TimeZone]
  def seaLevelStatus: ZIO[Any, Nothing, SeaLevelStatus] // TODO Move this out?
  def currentCountry: ZIO[Any, Nothing, Country]
```

Now that we have basic `Location`-awareness, we can build more domain-specific logic on top of it.

```scala mdoc
  
case class Slope(degrees: Float)
  
trait Topography: 
  def slope: ZIO[Location, Nothing, Slope]
  
```

```scala mdoc
case class Rainfall(inches: Int)
  
trait Almanac:
  def averageAnnualRainfail: ZIO[Location, Nothing, Rainfall]

```

{{TODO: GPS, City, Country, etc}}