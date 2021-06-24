package HelloZio

import zio.*
import zio.{UIO, ZIO}
import zio.console.{putStrLn}
import exIOError.errorAtNPerc

import java.io.IOException

object directoryExample extends zio.App{

  //This example will model a database with a list of employees and their information.
  case class employee(ID:Int, firstName:String, lastName: String, department:String):

    def getName:String =
      val name = s"$firstName $lastName"
      name

    override def toString =
      s"Name: $firstName $lastName. Department: $department. ID: $ID \n"

    def map =
      this

  def finalizer(source: scala.io.Source) = //Define the finalizer behavior here
    UIO.effectTotal {
      println("Finalizing: Closing file reader")
      source.close //Close the input source
    }

  val readFileContents: ZIO[Any, Throwable, Vector[String]] =
    ZIO(
      scala.io.Source.fromFile("src/main/scala/HelloZio/firmData.csv")
    ) //Open the file to read its contents
      .bracket(finalizer) {
        bufferedSource => //Use the bracket method with the finalizer defined above to define behavior on fail.

          val lines =
            for line <- bufferedSource.getLines
              yield line
          //This is where you can set the error likely hood
          //This models a fatal IOException
          errorAtNPerc(10)  //ie, 10 % chance to fail...
          ZIO.succeed(Vector() ++ lines)
      }

 // Read a line, and return an employee object
  def linesToEmps(lines:Vector[String]):Vector[employee] =
    val logic = for
      line <- lines
      emp = lineToEmp(line)
    yield emp
    logic

  def lineToEmp(line:String):employee =
    val parts:Array[String] = safeSplit(line, ",")
    val emp = employee(parts(0).toInt, parts(1), parts(2), parts(3))
    emp

  //This function deals with split() complications with the null safety element of the sbt.
   def safeSplit(line:String, key:String) =
     val nSplit = line.split(key)
     val arr = nSplit match
       case x:Null => Array[String]("1","2","3")
       case x:Array[String | Null] => x
     arr.collect {
       case s: String => s
     }

  //Compile list of emp data
  def compileEmps:ZIO[Any, Any, Vector[employee]] =
    for
      lines <- readFileContents
      emps = linesToEmps(lines)
    yield emps


  def run(args: List[String]) =
    val logic = for
      emps <- compileEmps
      _ <- putStrLn(emps.toString)
    yield ()


    logic.exitCode


}
