package experiments

import zio.*
import zio.test.*

case object ErrorObject1:
  val msg = "Failed: ErrorObject1"
case object ErrorObject2 extends Exception("Failed: ErrorObject2")

def matchTo1(n: Int) =
  if n == 1 then
    ZIO.fail("Failed at 1")
  else
    ZIO.succeed("Passed 1")

def matchTo2(n: Int) =
  if n == 2 then
    ZIO.fail(ErrorObject1)
  else
    ZIO.succeed("Passed 2")

def matchTo3(n: Int) =
  if n == 3 then
    ZIO.fail(ErrorObject2)
  else
    ZIO.succeed("Passed 3")

def completeTo(n: Int) =
  defer:
    val r1 = matchTo1(n).run
    val r2 = matchTo2(n).run
    val r3 = matchTo3(n).run
    printLine(s"Success: $r1 $r2 $r3").run

object FailVarieties extends ZIOSpecDefault:
  def spec =
    suite("Suite of Tests")(
      test("one"):
        defer:
          completeTo(1).run
          assertCompletes
      ,
      test("two"):
        defer:
          completeTo(2).run
          assertCompletes
      ,
      test("three"):
        defer:
          completeTo(3).run
          assertCompletes
      ,
      test("four"):
        defer:
          completeTo(4).run
          assertCompletes
      ,
    )
