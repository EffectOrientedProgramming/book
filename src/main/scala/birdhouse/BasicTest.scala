package birdhouse
import zio.test.*

object Basic extends ZIOSpecDefault:
  def spec =
    test("basic"):
      assertTrue(1 == 1)


object Basic2 extends ZIOSpecDefault:
  def spec =
    test("basic2"):
      assertTrue(1 != 1) // This is ignored
      assertTrue(1 == 1)

object Basic3 extends ZIOSpecDefault:
  def spec =
    test("basic3"):
      // Multiple boolean expressions:
      assertTrue(true, true, true)

import zio.Console.*

// Test can be an Effect as long as the final expression is an assertion.
// The Effect is automatically run by the test framework.
object Basic4 extends ZIOSpecDefault:
  def spec =
    test("basic4"):
      defer:
        printLine("testing basic4").run
        assertCompletes

// Can extract the Effect:
val basic5 =
  defer:
    printLine("testing basic5").run
    assertCompletes

object Basic5 extends ZIOSpecDefault:
  def spec =
    test("basic5"):
      basic5

val basic6 =
  defer:
    printLine("testing basic6").run
    assertTrue(1 == 1)

object Basic6 extends ZIOSpecDefault:
  def spec =
    suite("Creating Suites of Tests")(
      test("basic5 in suite"):
        basic5,
      test("basic6 in suite"):
        basic6,
    )
// Note that tests are run in parallel so output does not appear in the order the tests are listed