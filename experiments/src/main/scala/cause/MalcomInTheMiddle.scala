package cause

object MalcomInTheMiddle extends ZIOAppDefault:
  def run =

    def turnOnLights() = throw new BurntBulb()
    class BurntBulb() extends Exception

    def getNewBulb() = throw new WobblyShelf()
    class WobblyShelf() extends Exception

    def grabScrewDriver() =
      throw new SqueakyDrawer()
    class SqueakyDrawer() extends Exception

    def sprayWD40() = throw new EmptyCan()
    class EmptyCan() extends Exception

    def driveToStore() = throw new DeadCar()
    class DeadCar() extends Exception

    def repairCar() = throw new Nagging()
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
                        try repairCar()
                        finally
                          ZIO
                            .debug(
                              "What does it look like I'm doing?!"
                            )
                            .exitCode
    finally
      println
    end try
//    finally
//      ZIO
//        .debug(
//          "What does it look like I'm doing?!"
//        )
//    .exitCode

  end run

/** try { turnOnLights } catch { case
  * burntLightBulb => try {
  */
end MalcomInTheMiddle
