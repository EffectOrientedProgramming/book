package HelloZio

import HelloZio.CalculatorExample.input

import java.io.IOException
import zio.Console.{readLine, printLine}
import zio.Console
import fakeEnvironmentInstances.FakeConsole
import zio.Console
import zio.{IO, Ref, Runtime, ZIO, ZLayer, Has}

enum ArithmaticOperation(a: Float, b: Float):

  case Add(first: Float, second: Float)
      extends ArithmaticOperation(first, second)

  case Divide(first: Float, second: Float)
      extends ArithmaticOperation(first, second)

// def calculateX(): ZIO[Any, Throwable |
  // ArithmeticException, String]

  // This a more complicated example of using ZIO
  // to create a safely running program.
  // This is a simple calculator app that takes
  // in an opperation index, and two numbers.
  // It then prints the resulting calculation of
  // the two numbers based on the operation
  // index.

  // The ZIO are used in several ways. The ZIO
  // are used to ensure propper IO Exception and
  // error handling,
  // and they are used handle invalid inputs from
  // the user.

  def calculate(): ZIO[
    Any,
    Throwable | ArithmeticException,
    String
  ] = // This in an function used in calculations implemented below.
    this match
      case Add(first, second) =>
        ZIO {
          s"Adding $first and $second: ${first - second}"
        }
      case Divide(first, second) =>
        if (second != 0.0)
          ZIO {
            s"Dividing $first by $second: ${first / second}"
          }
        else
          ZIO.fail(
            new ArithmeticException(
              "divide by 0"
            )
          )
end ArithmaticOperation

object ArithmaticOperation: //This in an object used in calculations implemented below.

  def fromInt(
      index: Int
  ): (Float, Float) => ArithmaticOperation =
    index match
      case 1 =>
        Add.apply
      case 2 =>
        Divide.apply
      case _ =>
        throw new RuntimeException("boom")

// extends zio.App
object CalculatorExample extends zio.App:

  def input: ZIO[ // This function prompts and accepts the input from the user.
    Has[Console],
    IOException,
    Vector[String]
  ] =
    for
      _ <-
        printLine(""" ~~~~~~~~~~~~~~~~
          | Input Option:
          | 1) Add
          | 2) Subtract
          | 3) Multiply
          | 4) Devide
          |""")
      in <-
        readLine // User inputs the operation index
      _ <- printLine(s"input: ${in}")
      _ <- printLine("Enter first number: ")
      firstNum <-
        readLine // User inputs first and second number
      _ <- printLine("Enter second number: ")
      secondNum <- readLine
    yield Vector(
      in,
      firstNum,
      secondNum
    ) // The function returns a ZIO that succeeds with a vector of Strings

  def operate( // This function takes in the inputs, and processes them to ensure they are valid.
      // The function then returns a String
      // statement of the calculation.
      input: Vector[String]
  ): ZIO[
    Any,
    String |
      NumberFormatException |
      ArithmeticException |
      Throwable,
    String
  ] =
    for
      (number1, number2) <-
        ZIO {
          (input(1).toFloat, input(2).toFloat)
        } // The inputs are cast as Floats, and passed into a ZIO object.
      result <-
        ArithmaticOperation // This object, defined above, processes the operation index, and passes the according calculation to the function calculate.
          .fromInt(input(0).toInt)(
            number1,
            number2
          )
          .calculate() // calculate takes the input numbers from ArithmaticOperation, and creates the return statement
// _ <- printLine("Typed, parse operation: "
    // +
    // operation)
//      output <-
//        input(0) match
//          case "2" =>
//            ZIO {
// s"Subtracting $number1 and $number2:
    // ${number1 - number2}"
//            }
//          case "3" =>
//            ZIO {
// s"Multiplying $number1 and $number2:
    // ${number1 * number2}"
//            }
// case badIndex => ZIO.fail("unknown program
    // index: " + badIndex)
    yield result

  def run(args: List[String]) =
    println("In tester")
    val stringRef =
      Ref.make(
        Seq("1", "2", "3")
      ) // This is used in the automation software for running examples.

    val operated =
      for
        // console <-
        // FakeConsole.createConsoleWithInput(Seq("1",
        // "24", "8"))
        console <-
          FakeConsole.withInput(
            "2",
            "96",
            "8"
          ) // Run this program with the following inputs
        i <-
          input.provideLayer(
            ZLayer.succeed(console)
          )
        output <-
          operate(i).catchAll {
            case x: String =>
              ZIO("Input failure: " + x)
            case x: Throwable =>
              ZIO("toFloat failure: " + x)
          }
        _ <- printLine(output)
      yield ()

    operated.exitCode
  end run
end CalculatorExample
