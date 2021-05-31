package Parallelism

import antipatterns.SomeNewClass

import java.io.IOException
import zio.console.{Console, getStrLn, putStrLn}
import zio.{Fiber, IO, Runtime, UIO, ZIO, ZLayer}

import scala.io.Source._

object Finalizers extends zio.App{

  val importedFile: ZIO[Console, IOException, Vector[String]] =
    val bufferedSource = scala.io.Source.fromFile("src/main/scala/Parallelism/csvFile.csv")

    val lines = for
      line <- bufferedSource.getLines
    yield line
    ZIO.succeed(Vector() ++ lines)


  def run(args: List[String]) = //Use App's run function
    println("In main")

    val ioExample:ZIO[Console, IOException, Unit] =
      for
        i <- importedFile
        vector = i.flatMap(j => toString)
        _ <- putStrLn(vector.toString)
      yield ()
    ioExample.exitCode //Call the Zio with exitCode.





}
