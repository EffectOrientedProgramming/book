package fpBuildingBlocks

// Higher Order functions are functions that
// accept other functions as
// parameters.

object higherOrderFunctions:

  def foo1: Unit = //[f: Unit => Unit]
    println("I am function 1!!!")

  def foo2(x: Int): Unit = //[f: Int => Unit]
    println(s"I was given $x!!!")

  def add(
      x: Int,
      y: Int
  ): Int = //[f: (Int, Int) => Int]
    val added = x + y
    println(s"$x + $y = $added!!!")
    added

  def sub(
      x: Int,
      y: Int
  ): Int = //[f: (Int, Int) => Int]
    val subtracted = x - y
    println(s"$x - $y = $subtracted!!!")
    subtracted

  // The paramters of higherOrder are a function
  // f, that takes 2 Int,
  // and returns an Int, Int x, and Int y
  def higherOrder(
      f: (Int, Int) => Int,
      x: Int,
      y: Int
  ) =
    val mathed = f(x, y)
    println(
      s"I was given a function, Int $x and Int $y. \nThe output is $mathed"
    )
    mathed
  end higherOrder

  @main
  def higherOrders =
    val add3n2 =
      higherOrder(
        add,
        3,
        2
      ) //Here, we are passing in a function as a parameter
    println("\n")
    val sub3n2 = higherOrder(sub, 3, 2)
  end higherOrders
end higherOrderFunctions

// There are several higher order fucntions you
// probably already use!
// foreach(), map(), and flatMap() all take in
// functions as parameters.
