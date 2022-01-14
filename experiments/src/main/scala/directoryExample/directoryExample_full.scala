package directoryExample

import exIOError.errorAtNPerc
import zio.{UIO, ZIO}
import zio.Console.{getStrLn, printLine}
import java.io.IOException

object directoryExample_full
    extends zio.ZIOAppDefault:

  // This example will model a database with a
  // list of employees and their information.

  // This example covers a lot of ZIO tools. It
  // covers finalizers, several types of
  // error handling(fatal and non-fatal errors),
  // for comprehensions,
  // functional programming style (ie
  // composition, recursion, pure core/ effectful
  // outside, ect...)

// The fatal error type is a possible
  // IOException. The function errorAtNPerc will
  // trigger
  // and IO exception at the likelihood of n%.
  // The error handling for this is a
  // retry/schedule feature.
  // The non fatal error is for when a search
  // function does not find the target and throws
  // an error.
  // This is handled by a catch block.

  case class Employee(
      ID: Int,
      firstName: String,
      lastName: String,
      department: String
  ):

    def getName: String =
      val name = s"$firstName $lastName"
      name

    override def toString =
      s"Name: $firstName $lastName. Department: $department. ID: $ID \n"

    def map = this

  def finalizer(
      source: scala.io.Source
  ) = // Define the finalizer behavior here
    UIO.succeed {
      println("Finalizing: Closing file reader")
      source.close // Close the input source
    }

  // TODO Wyett: With the current setup, this
  // means that our Github Actions will fail 10%
  // of the time. Consider a
  // FakeRandom that behaves similarly to our
  // FakeConsole. So we could do the 10% failure
  // when running locally, but
  //  have a 100% success rate on Github
  val readFileContents
      : ZIO[Any, Throwable | IOException, Vector[
        String
      ]] =
    ZIO(
      scala
        .io
        .Source
        .fromFile(
          "src/main/scala/directoryExample/firmData.csv"
        )
    ) // Open the file to read its contents
      .acquireReleaseWith(finalizer) {
        bufferedSource => // Use the bracket method with the finalizer defined above to define behavior on fail.

          val lines =
            for
              line <- bufferedSource.getLines
            yield line
          // This is where you can set the error
          // likely hood
          // This models a fatal IOException
          errorAtNPerc(
            100
          ) // ie, 10 % chance to fail...
          ZIO.succeed(Vector() ++ lines)
      }

  // Read a line, and return an employee object
  def linesToEmployees(
      lines: Vector[String]
  ): Vector[Employee] =
    val logic =
      for
        line <- lines
        emp = lineToEmp(line)
      yield emp
    logic

  def lineToEmp(line: String): Employee =
    val parts: Array[String] =
      safeSplit(line, ",")
    val emp =
      Employee(
        parts(0).toInt,
        parts(1),
        parts(2),
        parts(3)
      )
    emp

  // This function deals with split()
  // complications with the null safety element
  // of the sbt.
  def safeSplit(line: String, key: String) =
    val nSplit = line.split(key)
    val arr =
      nSplit match
        case null =>
          Array[String]("1", "2", "3")
        case x: Array[String | Null] =>
          x
    arr.collect { case s: String =>
      s
    }

  // Compile list of emp data
  def compileEmps
      : ZIO[Any, Any, Vector[Employee]] =
    for
      lines <-
        readFileContents.retryN(
          5
        ) // An attempt to open the file occurs 5 times.
      emps = linesToEmployees(lines)
    yield emps

  case class EmpNotFound(message: String)

  // This function uses recursion to search the
  // list of employees for the given ID.
  // findEmp is a wrapper function for iterate,
  // which is the actual recursive function
  // iterate returns a monad. Either the ID was
  // found, or it wasn't.
  def findEmp(
      ID: Int,
      employees: Vector[Employee]
  ): ZIO[Any, EmpNotFound, Employee] =
    def iterate(
        index: Int,
        emps: Vector[Employee],
        targetID: Int
    ): ZIO[Any, EmpNotFound, Employee] =
      if (emps(index).ID == targetID)
        ZIO.succeed(emps(index))
      else if (index == 0)
        ZIO.fail(
          new EmpNotFound(
            s"Employee with ID $ID does not exit in the firm directory."
          )
        )
      else
        iterate(index - 1, emps, targetID)
    iterate(employees.length - 1, employees, ID)
  end findEmp

  def findEmp( // This is an overloaded function. The compiler can identify the correct 'findEmp' function by looking at the parameters used
      name: String,
      employees: Vector[Employee]
  ): ZIO[Any, EmpNotFound, Employee] =
    def iterate( // Example of tail recursion (linear) search
        index: Int,
        employees: Vector[Employee],
        targetName: String
    ): ZIO[Any, EmpNotFound, Employee] =
      if (employees(index).getName == targetName)
        ZIO.succeed(employees(index))
      else if (index == 0)
        ZIO.fail(
          new EmpNotFound(
            s"Employee with name $targetName does not exit in the firm directory."
          )
        )
      else
        iterate(index - 1, employees, targetName)
    iterate(
      employees.length - 1,
      employees,
      name
    )
  end findEmp

// ///////////////////////////////////
  def run =
    val logic =
      for
        employees <-
          compileEmps // Note: Executable logic is very concise. The behavior is predefined elsewhere, and only just executed in the main.
        // _ <- println(emps.toString)
        searchedEmp <-
          findEmp(
            4,
            employees
          ) // look for different employees based on ID
        _ <-
          printLine(
            s"Looking for employee... \n" +
              searchedEmp.toString
          )
      yield ()

    logic
      // You can comment out this section if you
      // want to see what the code looks like
      // without
      // catch error handling...
      .catchSome(i =>
        i match
          case e: EmpNotFound =>
            printLine(
              "Target employee not in System..."
            )
      )
      .catchSomeDefect {
        case e: IOException =>
          printLine(
            "Unexpected IOExceptions are the worst..."
          )
        case e: Throwable =>
          printLine(s"Huh, wasn't expecting $e")
      }
      .exitCode
  end run
end directoryExample_full
// //////////////////////////////////
