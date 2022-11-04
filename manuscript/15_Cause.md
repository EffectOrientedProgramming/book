# Cause

`Cause` will track all errors originating from a single call in an application, regardless of concurrency and parallelism.

```scala
import zio._
import mdoc.unsafeRunPrettyPrint
val logic =
  ZIO
    .die(new Exception("Client connection lost"))
    .ensuring(
      ZIO.die(
        throw new Exception("Release Failed")
      )
    )
unsafeRunPrettyPrint(logic)
// Exception in thread "zio-fiber-200030" java.lang.Exception: Client connection lost
// 	at repl.MdocSession$MdocApp.$anonfun$1(15_Cause.md:12)
// 	at zio.ZIO$.die$$anonfun$1(ZIO.scala:3017)
// 	at zio.ZIO$.failCause$$anonfun$1(ZIO.scala:3088)
// 	at repl.MdocSession.MdocApp.<local MdocApp>.logic(15_Cause.md:12)
// 	at repl.MdocSession.MdocApp.<local MdocApp>.logic(15_Cause.md:17)
// 	at mdoc.MdocHelpers$package.wrapUnsafeZIOReportError(MdocHelpers.scala:80)
// 	at mdoc.MdocHelpers$package.wrapUnsafeZIOReportError(MdocHelpers.scala:93)
// 	at mdoc.MdocHelpers$package.unsafeRunPrettyPrint(MdocHelpers.scala:103)
// 	Suppressed: java.lang.Exception: Release Failed
// 		at repl.MdocSession$MdocApp.$anonfun$2$$anonfun$1(15_Cause.md:15)
// 		at zio.ZIO$.die$$anonfun$1(ZIO.scala:3017)
// 		at zio.ZIO$.failCause$$anonfun$1(ZIO.scala:3088)
// 		at repl.MdocSession.MdocApp.<local MdocApp>.logic(15_Cause.md:16)
// 		at repl.MdocSession.MdocApp.<local MdocApp>.logic(15_Cause.md:17)
// 		at mdoc.MdocHelpers$package.wrapUnsafeZIOReportError(MdocHelpers.scala:80)
// 		at mdoc.MdocHelpers$package.wrapUnsafeZIOReportError(MdocHelpers.scala:93)
// 		at mdoc.MdocHelpers$package.unsafeRunPrettyPrint(MdocHelpers.scala:103)
// Caused by: java.lang.Exception: Client connection lost
// 	at repl.MdocSession$MdocApp.$anonfun$1(15_Cause.md:12)
// 	at zio.ZIO$.die$$anonfun$1(ZIO.scala:3017)
// 	at zio.ZIO$.failCause$$anonfun$1(ZIO.scala:3088)
// 	at zio.internal.FiberRuntime.runLoop(FiberRuntime.scala:1115)
// 	at zio.internal.FiberRuntime.evaluateEffect(FiberRuntime.scala:384)
// 	at zio.internal.FiberRuntime.start(FiberRuntime.scala:1369)
// 	at zio.Runtime$UnsafeAPIV1.run(Runtime.scala:124)
```

Cause allows you to aggregate multiple errors of the same type

`&&`/`Both` represents parallel failures
`++`/`Then` represents sequential failures

Cause.die will show you the line that failed, because it requires a throwable
Cause.fail will not necessarily, because it can be any arbitrary type

## Avoided Technique - Throwing Exceptions

Now we will highlight the deficiencies of throwing `Exception`s.
The previous code might be written in this style:

```scala
import zio._
import mdoc.unsafeRunPrettyPrint
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
// thrownLogic: ZIO[Any, Throwable, Nothing] = Stateful(
//   trace = "repl.MdocSession.MdocApp.thrownLogic(15_Cause.md:42)",
//   onState = zio.ZIOCompanionVersionSpecific$$Lambda$15564/1398923689@3f5b19d8
// )
unsafeRunPrettyPrint(thrownLogic)
// Should handle errors
// res0: String = "java.lang.Exception: Release Failed"
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


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/cause/CauseBasics.scala
```scala
package cause

import zio._

object CauseBasics extends App:
//    ZIO.fail(Cause.fail("Blah"))
  println(
    (
      Cause.die(Exception("1")) ++
        (Cause.fail(Exception("2a")) &&
          Cause.fail(Exception("2b"))) ++
        Cause
          .stackless(Cause.fail(Exception("3")))
    ).prettyPrint
  )

object CauseZIO extends ZIOAppDefault:

  val x: ZIO[Any, Nothing, Nothing] =
    ZIO.die(Exception("Blah"))
  def run = ZIO.die(Exception("Blah"))

object LostInfo extends ZIOAppDefault:
  def run =
    ZIO.attempt(
      try
        throw new Exception(
          "Client connection lost"
        )
      finally
        try () // Cleanup
        finally
          throw new Exception(
            "Problem relinquishing to pool"
          )
    )

```


### experiments/src/main/scala/cause/MalcomInTheMiddle.scala
```scala
package cause

import zio.{ZIO, ZIOAppDefault}

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

```


### experiments/src/main/scala/cause/MutationTracking.scala
```scala
package cause

import zio.{Cause, IO, UIO, ZIO}
import zio.Console.*

class MutationTracking:
  enum Stage:
    case Hominini,
      Chimpanzee,
      Human

object TimelineFinally extends App:
  try throw new Exception("Straightened Spine")
  finally
    try throw new Exception("Less Hair")
    finally
      throw new Exception("Fine Voice Control")

object Timeline extends zio.ZIOAppDefault:
  val mutation1: UIO[Nothing] =
    ZIO.die(Exception("Straightened Spine"))
  val mutation2 = ZIO.die(Exception("Less Hair"))
  val mutation3 =
    ZIO.die(Exception("Fine voice control"))

  val timeline =
    mutation1
      .ensuring(mutation2)
      .ensuring(mutation3)

  def run = timeline.sandbox

```

            