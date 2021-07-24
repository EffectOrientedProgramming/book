package fpBuildingBlocks

// The purpose of this example is to show the
// reader how general functions work.
// How a function can be passed in a type as a
// parameter, and how it may effect the
// behavior of the function.
object generalFunctions {

  // TODO:This is commented out becuase the scala
  // formatter doesnt like it...

  /* //Most commom exmple of a general function:
   * Lists
   *
   * val intList = List[Int](1,2,3,4) //The Int
   * variable type is passed into the List
   * definition as a type parameter
   *
   * val stringList =
   * List[String]("1","2","3","4") //Likewise
   * with the String type.
   *
   * //A type is usually indicated through the
   * use of square brackets. The contents usually
   * use parens
   *
   * //This example displays how a function can
   * be defined without a specific type.
   * //The different types can be passed into the
   * function, and the function will behave
   * //differently depending on the type that was
   * passed in.
   * case class stringAdd(num1:String,
   * num2:String) case class intAdd(num1:Int,
   * num2:Int)
   *
   * def add(x:B) =
   * x match { case x:stringAdd => x.num1.toInt +
   * x.num2.toInt case x:intAdd => x.num1 +
   * x.num2 case x:_ => println("What is this?")
   * }
   *
   * @main def additionEx =
   * val sAdd = stringAdd("1", "2") val iAdd =
   * intAdd(1,2)
   *
   * val sAdded = add[stringAdd](sAdd) //The type
   * of the object is passed in as a param val
   * iAdded = add[intAdd](iAdd)
   *
   * println(sAdded) println(iAdded) */
}
