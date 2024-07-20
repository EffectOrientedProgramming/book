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
        completeTo(1)
        assertCompletes
      ,
      test("two"):
        completeTo(2)
        assertCompletes
      ,
      test("three"):
        completeTo(3)
        assertCompletes
      ,
      test("four"):
        completeTo(4)
        assertCompletes
      ,
    )
