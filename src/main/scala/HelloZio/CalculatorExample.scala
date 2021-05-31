package HelloZio

import HelloZio.CalculatorExample.input

import java.io.IOException
import zio.console.{Console, getStrLn, putStrLn}
import fakeEnvironmentInstances.FakeConsole
import zio.console.Console.Service
import zio.{IO, Ref, Runtime, ZIO, ZLayer}

enum ArithmaticOperation(a: Float, b: Float):
  case Add(first: Float, second: Float)
      extends ArithmaticOperation(first, second)

  case Divide(first: Float, second: Float)
      extends ArithmaticOperation(first, second)

//  def calculateX(): ZIO[Any, Throwable | ArithmeticException, String]

  def calculate(): ZIO[Any, Throwable | ArithmeticException, String] =
    this match {
      case Add(first, second) =>
        ZIO {
          s"Adding $first and $second: ${first - second}"
        }
      case Divide(first, second) =>
        if (second != 0.0)
          ZIO {
            s"Dividing $first by $second: ${first / second}"
          }
        else ZIO.fail(new ArithmeticException("divide by 0"))
    }

object ArithmaticOperation:
  def fromInt(index: Int): (Float, Float) => ArithmaticOperation = index match {
    case 1 => Add.apply
    case 2 => Divide.apply
    case _ => throw new RuntimeException("bloom")
  }

//extends zio.App
object CalculatorExample extends zio.App {

  def input: ZIO[
    zio.console.Console,
    IOException,
    Vector[String]
  ] =
    for
      _ <- putStrLn(""" ~~~~~~~~~~~~~~~~
          | Input Option:
          | 1) Add
          | 2) Subtract
          | 3) Multiply
          | 4) Devide
          |""")
      in <- getStrLn
      _ <- putStrLn(s"input: ${in}")
      _ <- putStrLn("Enter first number: ")
      firstNum <- getStrLn
      _ <- putStrLn("Enter second number: ")
      secondNum <- getStrLn
    yield Vector(in, firstNum, secondNum)

  def operate(
      input: Vector[String]
  ): ZIO[
    Any,
    String | NumberFormatException | ArithmeticException | Throwable,
    String
  ] =
    for
      (number1, number2) <- ZIO { (input(1).toFloat, input(2).toFloat) }
      result <-
        ArithmaticOperation
          .fromInt(input(0).toInt)(number1, number2)
          .calculate()
//      _ <- putStrLn("Typed, parse operation: " + operation)
//      output <-
//        input(0) match
//          case "2" =>
//            ZIO {
//              s"Subtracting $number1 and $number2: ${number1 - number2}"
//            }
//          case "3" =>
//            ZIO {
//              s"Multiplying $number1 and $number2: ${number1 * number2}"
//            }
//          case badIndex => ZIO.fail("unknown program index: " + badIndex)
    yield result

  def run(args: List[String]) =
    println("In tester")
    val stringRef = Ref.make(Seq("1", "2", "3"))

    val operated =
      for
//        consoleInput <- Ref.make(Seq("1", "2", "3")) // Add 2 & 3
        consoleInput <- Ref.make(Seq("1", "24", "8"))
        console = FakeConsole.inputConsole(consoleInput)
        i <- input.provideLayer(ZLayer.succeed(console))
        output <- operate(i)
          .catchAll {
            case x: String    => ZIO("Input failure: " + x)
            case x: Throwable => ZIO("toFloat failure: " + x)
          }
        _ <- putStrLn(output)
      yield ()

    operated.exitCode

}
