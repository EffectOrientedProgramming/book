package directoryExample

import zio.{UIO, ZIO}
import zio.console.{putStrLn, getStrLn}
import java.io.IOException

import employee.*
import processingFunctions.*
import searchFunctions.*

object userInputLookup extends zio.App {

  //This example shows the possible modulatriy of scala and FP.
  //The programmer is easily able to make an organized system of functions
  //that can be put in their own files, then imported and used when nessessary.

  def run(args: List[String]) =
    val logic =
      for
        emps <-
          compileEmps //Note: Excecutable logic is very concise. The behavior is predefined elsewhere, and only just excecuted in the main.
        _ <- putStrLn("Input full employee name to retrieve from database:   ")
        empName <- getStrLn
        searchedEmp <- findEmp(
          empName,
          emps
        ) //look for different employees based on Input Name
        _ <- putStrLn(s"Looking for employee... \n" + searchedEmp.toString)
      yield ()

    logic
      //You can comment out this section if you want to see what the code looks like without
      //catch error handling...
      .catchSome(i =>
        i match
          case e: empNotFound => putStrLn("Target employee not in System...")
          case e: IOException =>
            putStrLn("Unexpected IOExceptions are the worst...")
          case e: Any => putStrLn(s"Huh, wasn't expecting $e")
      )
      .exitCode
}
