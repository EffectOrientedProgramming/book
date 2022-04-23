package cause

import zio.{ZEnv, ZIO, ZIOAppDefault}

object MalcomInTheMiddle extends ZIOAppDefault:
  def run =

    def turnOnLights() = ???
    class BurntBulb() extends Exception

    def getNewBulb() = ???
    class WobblyShelf() extends Exception

    def grabScrewDriver() = ???
    class SqueakyDrawer() extends Exception

    def sprayWD40() = ???
    class EmptyCan() extends Exception

    def driveToStore() = ???
    class DeadCar() extends Exception

    def repairCar() = ???
    class Nagging() extends Exception

    try
      turnOnLights()
    catch
      case burntBulb: BurntBulb =>
        try
          getNewBulb()
        catch
          case wobblyShelf: WobblyShelf =>
            try
              grabScrewDriver()
            catch
              case squeakyDrawer: SqueakyDrawer =>
                try
                  sprayWD40()
                catch
                  case emptyCan: EmptyCan =>
                    try
                      driveToStore()
                    catch
                      case deadCar: DeadCar =>
                        try
                          repairCar()
                        catch
                          case nagging: Nagging =>
                            ZIO
                              .succeed(
                                println(
                                  "What does it look like I'm doing?!"
                                )
                              )
                              .exitCode
    end try
  end run

/** try { turnOnLights } catch { case
  * burntLightBulb => try {
  */
end MalcomInTheMiddle
