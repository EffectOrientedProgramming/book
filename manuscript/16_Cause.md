# Cause TODO Consider putting inside error handling

`Cause` will track all errors originating from a single call in an application, regardless of concurrency and parallelism.

```scala
val logic =
  ZIO
    .die(new Exception("Connection lost"))
    .ensuring(
      ZIO.die(
        throw new Exception("Release Failed")
      )
    )
```
```scala
runDemo(logic)
// Defect: java.lang.Exception: Connection lost
```

Cause allows you to aggregate multiple errors of the same type

`&&`/`Both` represents parallel failures
`++`/`Then` represents sequential failures

Cause.die will show you the line that failed, because it requires a throwable
Cause.fail will not necessarily, because it can be any arbitrary type

## Manual demonstration of these operators

```scala
runDemo(
  Console.printLine(
    (
      Cause.die(Exception("1")) ++
        (Cause.fail(Exception("2a")) &&
          Cause.fail(Exception("2b"))) ++
        Cause
          .stackless(Cause.fail(Exception("3")))
    ).prettyPrint
  )
)
// Exception in thread "zio-fiber-" java.lang.Exception: 1
// 	at repl.MdocSession$MdocApp.$init$$$anonfun$4$$anonfun$1(16_Cause.md:31)
// 	at mdoctools.OurConsole$.printLine$$anonfun$1(OurConsole.scala:18)
// 	at scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
// 	at scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
// 	at zio.UnsafeVersionSpecific.implicitFunctionIsFunction$$anonfun$1(UnsafeVersionSpecific.scala:27)
// 	at zio.Unsafe$.unsafe(Unsafe.scala:37)
// 	at zio.ZIOCompanionVersionSpecific.succeed$$anonfun$1(ZIOCompanionVersionSpecific.scala:185)
// 	Suppressed: java.lang.Exception: 2a
// 		at repl.MdocSession$MdocApp.$init$$$anonfun$4$$anonfun$1(16_Cause.md:32)
// 		at mdoctools.OurConsole$.printLine$$anonfun$1(OurConsole.scala:18)
// 		at scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
// 		at scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
// 		at zio.UnsafeVersionSpecific.implicitFunctionIsFunction$$anonfun$1(UnsafeVersionSpecific.scala:27)
// 		at zio.Unsafe$.unsafe(Unsafe.scala:37)
// 		at zio.ZIOCompanionVersionSpecific.succeed$$anonfun$1(ZIOCompanionVersionSpecific.scala:185)
// 		Suppressed: java.lang.Exception: 3
// Exception in thread "zio-fiber-" java.lang.Exception: 1
// 	at repl.MdocSession$MdocApp.$init$$$anonfun$4$$anonfun$1(16_Cause.md:31)
// 	at mdoctools.OurConsole$.printLine$$anonfun$1(OurConsole.scala:18)
// 	at scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
// 	at scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
// 	at zio.UnsafeVersionSpecific.implicitFunctionIsFunction$$anonfun$1(UnsafeVersionSpecific.scala:27)
// 	at zio.Unsafe$.unsafe(Unsafe.scala:37)
// 	at zio.ZIOCompanionVersionSpecific.succeed$$anonfun$1(ZIOCompanionVersionSpecific.scala:185)
// 	Suppressed: java.lang.Exception: 2b
// 		at repl.MdocSession$MdocApp.$init$$$anonfun$4$$anonfun$1(16_Cause.md:33)
// 		at mdoctools.OurConsole$.printLine$$anonfun$1(OurConsole.scala:18)
// 		at scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
// 		at scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
// 		at zio.UnsafeVersionSpecific.implicitFunctionIsFunction$$anonfun$1(UnsafeVersionSpecific.scala:27)
// 		at zio.Unsafe$.unsafe(Unsafe.scala:37)
// 		at zio.ZIOCompanionVersionSpecific.succeed$$anonfun$1(ZIOCompanionVersionSpecific.scala:185)
// 		Suppressed: java.lang.Exception: 3
// ()
```

## Avoided Technique - Throwing Exceptions

Now we will highlight the deficiencies of throwing `Exception`s.
The previous code might be written in this style:

```scala
val thrownLogic =
  ZIO.attempt(
    try
      throw new Exception(
        "Client connection lost"
      )
    finally
      try () // Cleanup
      finally
        throw new Exception("Release Failed")
  )
// thrownLogic: ZIO[Any, Throwable, Nothing] = OnSuccess(
//   trace = "repl.MdocSession.MdocApp.thrownLogic(16_Cause.md:54)",
//   first = Sync(
//     trace = "repl.MdocSession.MdocApp.thrownLogic(16_Cause.md:54)",
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$14522/0x0000000103126c40@3fd0cc42
//   ),
//   successK = zio.ZIO$$$Lambda$14526/0x0000000103123840@7f16ae66
// )
runDemo(thrownLogic)
// java.lang.Exception: Release Failed
```

We will only see the later `pool` problem.
If we throw an `Exception` in our logic, and then throw another while cleaning up, we simply lose the original.
This is because thrown `Exception`s cannot be _composed_.

In a language that cannot `throw`, following the execution path is simple, following 2 basic rules:

    - At a branch, execute only the first match
    - Otherwise, Read everything from left-to-right, top-to-bottom, 

Once you add `throw`, the rules are more complicated

    - At a branch, execute only the first match
    - Otherwise, Read everything from left-to-right, top-to-bottom,
    - Unless we `throw`, which means immediately jumping through a different dimension away from the code you're viewing

### Linear reporting
Everything must be reported linearly, even in systems that are executing on different fibers, across several threads, amongst multiple cores.


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/16_Cause.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

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

