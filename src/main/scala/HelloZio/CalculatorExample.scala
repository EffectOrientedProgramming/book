package HelloZio

import java.io.IOException
import zio.console.{Console, getStrLn, putStrLn}
import fakeEnvironmentInstances.FakeConsole
import zio.console.Console.Service
import zio.{IO, Runtime, ZIO, ZLayer, Ref}

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
    input(0) match
      case "1" =>
        ZIO {
          s"Adding " + input(1) + " and " + input(2) + s": ${input(1).toFloat + input(2).toFloat}"
        }

      case "2" =>
        ZIO {
          s"Subtracting " + input(1) + " and " + input(2) + s": ${input(1).toFloat - input(2).toFloat}"
        }

      case "3" =>
        ZIO {
          s"Multiplying " + input(1) + " and " + input(2) + s": ${input(1).toFloat * input(2).toFloat}"
        }

      case "4" =>
        if (input(2).toFloat != 0.0)
          ZIO {
            s"Deviding " + input(1) + " by " + input(2) + s": ${input(1).toFloat / input(2).toFloat}"
          }
        else ZIO.fail(new ArithmeticException("Cannot devide by 0."))
      case badIndex => ZIO.fail("unknown program index: " + badIndex)

  def run(args: List[String]) =
    println("In tester")
    val stringRef = Ref.make(Seq("1", "2", "3"))

    val operated =
      for
//        consoleInput <- Ref.make(Seq("1", "2", "3")) // Add 2 & 3
        consoleInput <- Ref.make(Seq("4", "24", "8"))
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
