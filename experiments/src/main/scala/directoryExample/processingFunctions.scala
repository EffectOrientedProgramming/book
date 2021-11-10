package directoryExample

import zio.ZIO

object processingFunctions:

  // Read a line, and return an employee object
  def linesToEmps(
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
      emps = linesToEmps(lines)
    yield emps
end processingFunctions
