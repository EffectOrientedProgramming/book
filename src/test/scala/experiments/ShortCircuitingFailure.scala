package experiments

import zio.*
import zio.test.*

def matchTo(n: Int, limit: Int) =
  if n < limit then
    ZIO.succeed(s"Passed $n")
  else
    ZIO.fail(s"Failed at $n")

def completeToShortCircuit(n: Int) =
  defer:
    val r1 =
      matchTo(0, n).run
    printLine(r1).run
    val r2 =
      matchTo(1, n).run
    printLine(r2).run
    val r3 =
      matchTo(2, n).run
    printLine(r3).run
    // TODO Decide whether to just return r3 instead of a new string here
    ZIO.succeed(s"Passed all steps").run

object ShortCircuitingFailure extends ZIOSpecDefault:
  def spec =
    suite("Suite of Tests")(
      test("one"):
        defer:
          val result = completeToShortCircuit(1).flip.run
          assertTrue(result == "Failed at 1")
      ,
      test("two"):
        defer:
          val result = completeToShortCircuit(2).flip.run
          assertTrue(result == "Failed at 2")
      ,
      test("three"):
        defer:
          val result = completeToShortCircuit(3).run
          assertTrue(result == "Passed all steps")
      ,
    ) @@ TestAspect.sequential
end ShortCircuitingFailure
