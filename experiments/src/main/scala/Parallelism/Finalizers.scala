package Parallelism

import java.io.IOException
import zio.Console.printLine

import scala.io.Source.*

object Finalizers extends zio.ZIOAppDefault:

  // In this example, we create a ZIO that uses
  // file IO. It opens a file to read it, but
  // gets failed half way through.
  // We use a finalizer to ensure that even if
  // the ZIO fails unexpectedly, the file will
  // still be closed.

  def finalizer(
      source: scala.io.Source
  ) = // Define the finalizer behavior here
    ZIO.succeed {
      println("Finalizing: Closing file reader")
      source.close // Close the input source
    }

  val readFileContents
      : ZIO[Scope, Throwable, Vector[String]] =
    ZIO
      .acquireRelease(
        ZIO.succeed(
          scala
            .io
            .Source
            .fromFile(
              "src/main/scala/Parallelism/csvFile.csv"
            )
        )
      )(finalizer)
      .map {
        bufferedSource => // Use the bracket method with the finalizer defined above to define behavior on fail.

          val lines = bufferedSource.getLines

          if (
            true
          ) // Simulating an enexpected error/exception
            throw new IOException("Boom!")

          Vector() ++ lines
      }

  def run = // Use App's run function
    println("In main")

    val ioExample: ZIO[
      Scope,
      Throwable,
      Unit
    ] = // Define the ZIO contexts
      defer {
        val fileLines = readFileContents.run
        printLine(fileLines.mkString("\n"))
          .run // Combine the strings of the output vector into a single string, separated by \n
      }

    ioExample
      .catchAllDefect(exception =>
        printLine(
          "Ultimate error message: " +
            exception.getMessage
        )
      )
      .exitCode // Call the Zio with exitCode.
  end run
end Finalizers
