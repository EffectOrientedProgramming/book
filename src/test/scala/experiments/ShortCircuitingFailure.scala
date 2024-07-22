package experiments

import zio.*
import zio.direct.*
import zio.Console.*

def testLimit(n: Int, limit: Int) =
  println(s"testLimit($n, $limit)")
  if n < limit then
    ZIO.succeed(s"Passed $n")
  else
    println("n >= limit: testLimit failed")
    ZIO.fail(s"Failed at $n")

def shortCircuiting(n: Int) =
  defer:
    val r1 = testLimit(0, n).run
    printLine(s"-> n: $n, r1: $r1").run
    val r2 = testLimit(1, n).run
    printLine(s"-> n: $n, r2: $r2").run
    val r3 = testLimit(2, n).run
    printLine(s"-> n: $n, r3: $r3").run
    // TODO Decide whether to just return r3 instead of a new string here
    // ZIO.succeed(s"Passed all steps").run
    r3


object ShortCircuitingFailure1 extends ZIOAppDefault:
  def run =
    defer:
      val result0 = shortCircuiting(0).flip.run
      printLine(s"result0: $result0").run
      val result1 = shortCircuiting(1).flip.run
      printLine(s"result1: $result1").run
      val result2 = shortCircuiting(2).flip.run
      printLine(s"result2: $result2").run

//----------------------------------------------

//import zio.test.*
//
//object ShortCircuitingFailure2 extends ZIOSpecDefault:
//  def spec =
//    suite("Suite of Tests")(
//      test("one"):
//        defer:
//          val result = shortCircuiting(1).flip.run
//          assertTrue(result == "Failed at 1")
//      ,
//      test("two"):
//        defer:
//          val result = shortCircuiting(2).flip.run
//          assertTrue(result == "Failed at 2")
//      ,
//      test("three"):
//        defer:
//          val result = shortCircuiting(3).run
//          assertTrue(result == "Passed all steps")
//      ,
//    ) @@ TestAspect.sequential
