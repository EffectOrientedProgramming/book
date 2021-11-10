package directoryExample

import zio.ZIO

object searchFunctions:

  case class EmpNotFound(message: String)

  // This function uses recursion to search the
  // list of employees for the given ID.
  // findEmp is a wrapper function for itterate,
  // which is the actual recursive function
  // itterate returns a monad. Either the ID was
  // found, or it wasn't.
  def findEmp(
      ID: Int,
      emps: Vector[Employee]
  ): ZIO[Any, EmpNotFound, Employee] =
    ZIO
      .fromOption(emps.find(_.ID == ID))
      .mapError { case _ =>
        EmpNotFound(
          s"Employee with ID $ID does not exit in the firm directory."
        )
      }

//    def itterate(
//        index: Int,
//        emps: Vector[Employee],
//        targetID: Int
//    ): ZIO[Any, empNotFound, Employee] =
//      if (emps(index).ID == targetID)
//        ZIO.succeed(emps(index))
//      else if (index == 0)
//        ZIO.fail(
//          new empNotFound(
// s"Employee with ID $ID does not exit in the
  // firm directory."
//          )
//        )
//      else
//        itterate(index - 1, emps, targetID)
//    itterate(emps.length - 1, emps, ID)

  def findEmp( // This is an overloaded function. The compiler can identify the correct 'findEmp' function by looking at the parameters used
      name: String,
      emps: Vector[Employee]
  ): ZIO[Any, EmpNotFound, Employee] =
    ZIO
      .fromOption(emps.find(_.getName == name))
      .mapError { case _ =>
        EmpNotFound(
          s"Employee with ID $name does not exit in the firm directory."
        )
      }
end searchFunctions

// def iterate( //Example of tail recursion
// (linear) search
//                 index: Int,
//                 emps: Vector[Employee],
//                 targetName: String
//    ): ZIO[Any, empNotFound, Employee] =
//      if (emps(index).getName == targetName)
//        ZIO.succeed(emps(index))
//      else if (index == 0)
//        ZIO.fail(
//          new empNotFound(
// s"Employee with name $targetName does not
// exit in the firm directory."
//          )
//        )
//      else
//        iterate(index, emps, targetName)
//    iterate(emps.length - 1, emps, name)
//
