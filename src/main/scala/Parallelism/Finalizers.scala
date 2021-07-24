package Parallelism

import antipatterns.SomeNewClass

import java.io.IOException
import zio.console.{getStrLn, putStrLn, Console}
import zio.{
  Fiber,
  IO,
  Runtime,
  UIO,
  URIO,
  ZIO,
  ZLayer
}

import scala.io.Source._

object Finalizers extends zio.App:

  // In this example, we create a ZIO that uses
  // file IO. It opens a file to read it, but
  // gets failed half way through.
  // We use a finalizer to ensure that even if
  // the ZIO fails unexpectedly, the file will
  // still be closed.

  def finalizer(
      source: scala.io.Source
  ) = //Define the finalizer behavior here
    UIO.effectTotal {
      println("Finalizing: Closing file reader")
      source.close //Close the input source
    }

  val readFileContents
      : ZIO[Any, Throwable, Vector[String]] =
    ZIO(
      scala
        .io
        .Source
        .fromFile(
          "src/main/scala/Parallelism/csvFile.csv"
        )
    ) //Open the file to read its contents
      .bracket(finalizer) {
        bufferedSource => //Use the bracket method with the finalizer defined above to define behavior on fail.

          val lines =
            for
              line <- bufferedSource.getLines
            yield line

          if (
            true
          ) //Simulating an enexpected error/exception
            throw new IOException("Boom!")

          ZIO.succeed(Vector() ++ lines)
      }

  def run(
      args: List[String]
  ) = //Use App's run function
    println("In main")

    val ioExample: ZIO[
      Console,
      Throwable,
      Unit
    ] = //Define the ZIO contexts
      for
        fileLines <- readFileContents
        _ <-
          putStrLn(
            fileLines.mkString("\n")
          ) //Combine the strings of the output vector into a single string, separated by \n
      yield ()
    ioExample
      .catchAllDefect(exception =>
        putStrLn(
          "Ultimate error message: " +
            exception.getMessage
        )
      )
      .exitCode //Call the Zio with exitCode.
end Finalizers
