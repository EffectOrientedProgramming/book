package logging

import zio.logging.*
import zio.*
import zio.logging.LogFormat.{
  label,
  line,
  quoted,
  text
}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Logging extends ZIOAppDefault:

  lazy val minimal: LogFormat =
    label("message", quoted(line)).highlight

  lazy val locationLogger: LogFormat =
    location(new WackyGps) |-|
      label("message", quoted(line)).highlight

  lazy val coloredLogger =
    Runtime.removeDefaultLoggers >>>
      consoleLogger(
        ConsoleLoggerConfig(
//        LogFormat.colored
          locationLogger,
          LogFilter.logLevel(LogLevel.Info)
        )
      )

  def run = ZIO.log("Hi").provide(coloredLogger)

//  val timestamp: LogFormat = timestamp(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  def location(gps: Gps): LogFormat =
    text {
      gps.currentLocation().toString
    }
end Logging

enum Continent:
  case NorthAmerica,
    SouthAmerica,
    Europe,
    Asia,
    Antarctica,
    Australia,
    Africa

trait Gps:
  def currentLocation(): Continent

class WackyGps extends Gps:
  def currentLocation(): Continent =
    Continent
      .values
      .apply(
        scala
          .util
          .Random
          .between(0, Continent.values.length)
      )
