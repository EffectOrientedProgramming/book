package scalaBasics

object fold {

  //This set of exmples explains how the fold function works, and how to use it.

  //There are three types of folds: fold(), foldLeft, foldRight
  @main def foldEx1 =
    //All three take two arguments:
    val listEx = List(1, 2, 3, 4, 5, 6)
    //The first argument is where to start from, the second argument is a funciton that takes two parameters.
    //The first parameter is the acumulated or 'folded up' variable, and the second is the
    //next thing to be folded into the first variable.
    val folded = listEx.fold(0)((z, i) =>
      println(s"z = $z")
      println(s"i = $i \n")
      z + i
    )
    //The first parameter, 0, is where the folding will start from.
    //The second variable is a function taking z, and i. z being the previously accumulated
    //variable, and i being the next variable to be accumulated.
    println(folded)

  @main def foldEx2 =
    //foldLeft() and foldRight() fold from specific directions.
    //foldLeft() folds FROM the left.
    //foldRight() folds FROM the right.
    val letterList =
      List('a', 'b', 'c', 'd', 'e')
    println("Folding from left: ")
    val leftFolded =
      letterList.foldLeft("")((z, i) =>
        println(s"z = $z")
        println(s"i = $i \n")
        s"$z${i.toString} "
      )
    println(s"leftFolded = $leftFolded \n")

    println("Folding from right: ")
    val rightFolded =
      letterList.foldRight("")((i, z) =>
        println(s"z = $z")
        println(s"i = $i  \n")
        s"$z${i.toString}"
      )

    //NOTE: The order of the parameters in the passed in function are significant
    // between fold left and fold right.
    //With fold left, the left variable (A, b) is the accumulated variable.
    //With fold right, the right variable ( a, B) is the accumulated variable.
    //Thus, the accumulated var 'z' is on the right side of the parameters for the
    //passed in function.

    println(s"rightFolded = $rightFolded")
}

@main def foldEx3 =
  // Folding is not always just accumulating in some way. It can also just
  // be used to itterate through a List. Here is an exmple of using a foldLeft
  // to find the max value in a list.

  val numList = List(1, 4, 2, 10, 6, 3, 7, 9)

  val maxOfList =
    numList.foldLeft(numList(0))((z, i) =>
      println(s"z = $z")
      println(s"i = $i \n")
      if (i > z)
        i
      else
        z
    )
  //In this case, the 'accumulated' var is z, but instead of building up through the values,\
  // in the passed in function, z is compared to i, and which ever is greater is passed on as z.

  println(maxOfList)

@main def foldEx4 =
  //Variable type also becomes important when folding.
  //The accumulated variable must be of the nw type, and the
  // variable to be added is of the old type.
  val numList = List(1, 2, 3, 4, 5, 6, 7)

  val strVersion = numList.foldLeft(
    "": String
  )((z: String, i: Int) => s"$z ${i.toString}")

  println(strVersion)

@main def foldEx5 =
  //Folding can be a good way to make a list in functional Programming.

  val numList = List(1, 2, 3, 4, 5, 6, 7)

  val strList = numList.foldLeft(
    List.empty[String]
  )((z: List[String], i: Int) =>
    i.toString :: z
  )

  println(strList)
