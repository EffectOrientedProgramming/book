## cause

 

### experiments/src/main/scala/cause/MalcomInTheMiddle.scala
```scala
package cause

object MalcomInTheMiddle extends ZIOAppDefault:
  @annotation.nowarn
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

```


### experiments/src/main/scala/cause/MalcomInTheMiddleZ.scala
```scala
package cause

object MalcomInTheMiddleZ extends ZIOAppDefault:
  def run =
    def turnOnLights() = ZIO.fail(BurntBulb())
    class BurntBulb()
        extends Exception("Burnt Bulb!")

    def getNewBulb() =
      ZIO.attempt(
        throw new Exception("Wobbly Shelf!")
      )

    def grabScrewDriver() =
      ZIO.fail(Exception("SqueakyDrawer"))

    defer {
      turnOnLights()
        .catchAllCause(originalError =>
          getNewBulb().catchAllCause(bulbError =>
            grabScrewDriver()
              .mapErrorCause(screwDriverError =>
                (originalError ++ bulbError) ++
                  screwDriverError
              )
          )
        )
        .run
      ZIO.debug("Preserve failures!").run
    }.catchAllCause(bigError =>
      ZIO.debug(
        "Final error: " +
          simpleStructureAlternative(bigError)
      )
    )
  end run
end MalcomInTheMiddleZ

def simpleStructure(
    cause: Cause[Throwable]
): String =
  cause match
    case Cause.Empty =>
      ???
    case Cause.Fail(value, trace) =>
      value.getMessage
    case Cause.Die(value, trace) =>
      ???
    case Cause.Interrupt(fiberId, trace) =>
      ???
    case Cause.Stackless(cause, stackless) =>
      ???
    case Cause.Then(left, right) =>
      "Then(" + simpleStructure(left) + ", " +
        simpleStructure(right) + ")"
    case Cause.Both(left, right) =>
      ???

def simpleStructureAlternative(
    cause: Cause[Throwable]
): String =
  cause match
    case Cause.Fail(value, trace) =>
      value.getMessage
    case Cause.Then(left, right) =>
      simpleStructureAlternative(left) + " => " +
        simpleStructureAlternative(right)
    case Cause.Both(left, right) =>
      ???
    case _ =>
      ???

```


