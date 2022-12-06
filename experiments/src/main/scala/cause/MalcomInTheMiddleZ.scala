package cause

import zio.*

object MalcomInTheMiddleZ extends ZIOAppDefault:
  def run =
    def turnOnLights() = ZIO.fail(BurntBulb())
    class BurntBulb() extends Exception("Burnt Bulb!")

    def getNewBulb() = ZIO.attempt(throw new Exception("Wobbly Shelf!"))

    def grabScrewDriver() =
      ZIO.fail(Exception("SqueakyDrawer"))

    (for
      _ <-
        turnOnLights().catchAllCause(originalError =>
          getNewBulb()
            .catchAllCause(bulbError =>
              grabScrewDriver()
                .mapErrorCause(screwDriverError =>
                    (originalError ++ bulbError)
                      ++ screwDriverError
            )
          )
        )
      _ <- ZIO.debug("Preserve failures!")
    yield ()
      ).catchAllCause(bigError => ZIO.debug("Final error: " + simpleStructure(bigError)))

def simpleStructure(cause: Cause[Throwable]): String =
  cause match
    case Cause.Empty => ???
    case Cause.Fail(value, trace) =>
      value.getMessage
    case Cause.Die(value, trace) => ???
    case Cause.Interrupt(fiberId, trace) => ???
    case Cause.Stackless(cause, stackless) => ???
    case Cause.Then(left, right) =>
      "Then(" + simpleStructure(left) + ", " + simpleStructure(right) + ")"
    case Cause.Both(left, right) => ???