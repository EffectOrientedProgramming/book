package Parallelism

import antipatterns.SomeNewClass

import java.io.IOException
import zio.console.{Console, getStrLn, putStrLn}
import zio.{Fiber, IO, Runtime, UIO, ZIO, ZLayer}

import scala.io.Source._

object Finalizers extends zio.App{

  val readFileContents: ZIO[Console, IOException, Vector[String]] =

    ZIO.succeed {
      val bufferedSource = scala.io.Source.fromFile("src/main/scala/Parallelism/csvFile.csv")

      val lines = for
        line <- bufferedSource.getLines
      yield line

      if (true)
        throw new IOException("Boom!")

      Vector() ++ lines
    }.ensuring(finalizer)

  val finalizer =  //Define the finalizer behavior here
    UIO.effectTotal{
      println("Finalizing: Closing file reader")
      //bufferedSource.close
    }



  def run(args: List[String]) = //Use App's run function
    println("In main")

    val ioExample:ZIO[Console, IOException, Unit] =
      for
      //First way of using a finalizer: When executing/interpreting a ZIO, use the .ensuring method with the finalizer value name.
        fileLines <- readFileContents
        _ <- putStrLn(fileLines.mkString("\n"))
      yield ()
    ioExample.exitCode //Call the Zio with exitCode.





}
