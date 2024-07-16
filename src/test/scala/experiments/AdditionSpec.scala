package experiments

import zio.*
import zio.test.{test, *}

def add(a: Int, b: Int): Int =
  a + b

def loadTestData =
  ZIO.attemptBlocking(
    scala.io.Source
      .fromResource("test-data.csv")
      .getLines()
      .toList
      .map(_.split(',').map(_.trim))
      .map(
        i =>
          (
            i(0).toInt, i(1).toInt, i(2).toInt,
          )
      )
  )

def makeTest(a: Int, b: Int, expected: Int) =
  test(s"test add($a, $b) == $expected"):
    assertTrue(add(a, b) == expected)

def makeTests =
  loadTestData.map:
    testData =>
      testData.map:
        case (a, b, expected) =>
          makeTest(a, b, expected)

object AdditionSpec extends ZIOSpecDefault:
  override def spec =
    suite("add")(makeTests)
