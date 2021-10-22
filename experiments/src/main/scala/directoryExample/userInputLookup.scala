package directoryExample

import zio.{UIO, ZIO, ZLayer}
import zio.Console.{readLine, printLine}

import java.io.IOException
import Employee.*
import fakeEnvironmentInstances.FakeConsole
import processingFunctions.*
import searchFunctions.*

object userInputLookup extends zio.App:

  // This example shows the possible modulatriy
  // of scala and FP.
  // The programmer is easily able to make an
  // organized system of functions
  // that can be put in their own files, then
  // imported and used when nessessary.

  def run(args: List[String]) =
    val logic =
      for
        emps <-
          compileEmps //Note: Excecutable logic is very concise. The behavior is predefined elsewhere, and only just excecuted in the main.
        _ <-
          printLine(
            "Input full employee name to retrieve from database:   "
          )
        empName <- readLine
        searchedEmp <-
          findEmp(
            empName,
            emps
          ) //look for different employees based on Input Name
        _ <-
          printLine(
            s"Looking for employee... \n" +
              searchedEmp.toString
          )
      yield ()
    (
      for
        console <-
          FakeConsole.withInput(
            "2",
            "96",
            "8"
          ) //Run this program with the following inputs

        _ <-
          logic
            .provideLayer(
              ZLayer.succeed(console)
            )
            // You can comment out this section
            // if you want to see what the code
            // looks like without
            // catch error handling...
            .catchSome(i =>
              i match
                case e: EmpNotFound =>
                  printLine(
                    "Target employee not in System..."
                  )
            )
            .catchSomeDefect(i =>
              i match
                case e: IOException =>
                  printLine(
                    "Unexpected IOExceptions are the worst..."
                  )
                case e: Throwable =>
                  printLine(
                    s"Huh, wasn't expecting $e"
                  )
            )
      yield ()
    ).exitCode
  end run
end userInputLookup
