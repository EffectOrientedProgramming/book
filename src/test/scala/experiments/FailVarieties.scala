package experiments

import zio.*
import zio.test.*

case object ObjectX
case object ExceptionX extends Exception:
  override def toString: String = "ExceptionX"

def failureTypes(n: Int) =
  n match
    case 0 =>
      ZIO.fail("String fail")
    case 1 =>
      ZIO.fail(ObjectX)
    case _ =>
      ZIO.fail(ExceptionX)

object TestFailureTypes extends ZIOAppDefault:
  def run =
    defer:
      val r0 = failureTypes(0).flip.run
      printLine(s"r0: $r0").run
      val r1 = failureTypes(1).flip.run
      printLine(s"r1: $r1").run
      val r2 = failureTypes(2).flip.run
      printLine(s"r2: $r2").run

// -------------------------------------------------

def matchTo1(n: Int) =
  if n == 1 then
    defer:
      printLine("Failed at 1").run
      ZIO.fail("Failed at 1")
  else
    defer:
      printLine("Passed 1").run
      ZIO.succeed("Passed 1")

def matchTo2(n: Int) =
  if n == 2 then
    defer:
      printLine("Failed at 2").run
      ZIO.fail(ObjectX)
  else
    defer:
      printLine("Passed 2").run
      ZIO.succeed("Passed 2")

def matchTo3(n: Int) =
  if n == 2 then
    defer:
      printLine("Failed at 3").run
      ZIO.fail(ExceptionX)
  else
    defer:
      printLine("Passed 3").run
      ZIO.succeed("Passed 3")

def completeTo(n: Int) =
  defer:
    val r1 =
      matchTo1(n).catchAll:
        e => ZIO.debug(s"Caught: $e").as(e)
    printLine(r1).run
    val r2 =
      matchTo2(n).catchAll:
        e => ZIO.debug(s"Caught: $e").as(e)
    printLine(r2).run
    val r3 =
      matchTo3(n).catchAll:
        e => ZIO.debug(s"Caught: $e").as(e)
    printLine(s"Success: $r1 $r2 $r3").run

object FailVarieties extends ZIOSpecDefault:
  def spec =
    suite("Suite of Tests")(
      test("one"):
        defer:
          completeTo(1).run
          printLine("completeTo(1) ran").run
          assertCompletes
      ,
      test("two"):
        defer:
          completeTo(2).run
          printLine("completeTo(2) ran").run
          assertCompletes
      ,
      test("three"):
        defer:
          completeTo(3).run
          printLine("completeTo(3) ran").run
          assertCompletes
      ,
      test("four"):
        defer:
          completeTo(4).run
          printLine("completeTo(4) ran").run
          assertCompletes,
    )
end FailVarieties
