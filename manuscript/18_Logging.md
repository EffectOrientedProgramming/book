# Logging

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/18_Logging.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/logging/Logging.scala
```scala
package logging

import zio.logging.*
import zio.logging.LogFormat.{
  label,
  line,
  quoted,
  text
}

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

```

