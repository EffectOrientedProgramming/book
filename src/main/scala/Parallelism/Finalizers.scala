package Parallelism

import antipatterns.SomeNewClass

import java.io.IOException
import zio.console.{Console, getStrLn, putStrLn}
import zio.{Fiber, IO, Runtime, UIO, ZIO, URIO, ZLayer}

import scala.io.Source._

object Finalizers extends zio.App {

  def finalizer(source: scala.io.Source) = //Define the finalizer behavior here
    UIO.effectTotal {
      println("Finalizing: Closing file reader")
      source.close
    }

  val readFileContents: ZIO[Any, Throwable, Vector[String]] =
    ZIO(scala.io.Source.fromFile("src/main/scala/Parallelism/csvFile.csv"))
      .bracket(finalizer) { bufferedSource =>

        val lines =
          for line <- bufferedSource.getLines
          yield line

        if (true)
          throw new IOException("Boom!")

        ZIO.succeed(Vector() ++ lines)
      }

  def run(args: List[String]) = //Use App's run function
    println("In main")

    val ioExample: ZIO[Console, Throwable, Unit] =
      for
        //First way of using a finalizer: When executing/interpreting a ZIO, use the .ensuring method with the finalizer value name.
        fileLines <- readFileContents
        _ <- putStrLn(fileLines.mkString("\n"))
      yield ()
    ioExample.exitCode //Call the Zio with exitCode.

}
