package cause

object MalcomInTheMiddleZ extends ZIOAppDefault:
  def run =
    def turnOnLights() =
      ZIO.fail(BurntBulb())
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
        .catchAllCause(
          originalError =>
            getNewBulb().catchAllCause(
              bulbError =>
                grabScrewDriver().mapErrorCause(
                  screwDriverError =>
                    (originalError ++
                      bulbError) ++
                      screwDriverError
                )
            )
        )
        .run
      ZIO.debug("Preserve failures!").run
    }.catchAllCause(
      bigError =>
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
