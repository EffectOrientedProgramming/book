package scalaBasics

object forComprehension {

  //This example goes through the basics of the for comprehension.
  //Intitially, it will be compared to the for loop equivelant.

  val numbers = Vector(1, 2, 3, 4, 5, 6)

  //for loop
  def forloopEx =
    println("For Loop: ")
    for (i <- Range(0, 6)) {
      val value = numbers(i)
      print(s"$value, ")
    }
    println()

  //for comprehension
  def forCompEx =
    println("For Comprehension: ")
    for {
      i <- numbers
    } print(s"$i" + ", ")
    println()

  //Example function: This function is slightly more complex.
  //It demonstrates how to filter elements in a for comprehension.
  def evenGT5v1(v: Vector[Int]): Vector[Int] = {
    // 'var' so we can reassign 'result':
    println("Finding values greater than 5 and even: ")
    var result = Vector[Int]()
    for {
      n <- v //Take the input value v, and itterate through each element
      if n > 5 //If n is greater than 5
      if n % 2 == 0 //and n is divisible by 2
    } result = result :+ n //Then add n to the result list
    result //return result
  }

//To remove the use of a var, and simplify the code, you can use the yield keyword.
  //'Yield'ing will create a list of all the values that satasfied the critria.
  //'Yield'ing essentially creates the list in place.
  def evenGT5v2(v: Vector[Int]): Vector[Int] = {
    // 'var' so we can reassign 'result':
    println("Finding values greater than 5 and even: ")
    for {
      n <- v //Take the input value v, and itterate through each element
      if n > 5 //If n is greater than 5
      if n % 2 == 0 //and n is divisible by 2
    } yield n //create a list of the values of n.
  }

  @main def run() =
    forloopEx
    forCompEx

    val v = Vector(1, 2, 3, 5, 6, 7, 8, 10, 13, 14, 17)
    println(evenGT5v1(v))
    println(evenGT5v2(v))

  //For comprehensions can also be used to string together multiple events.
  //In some cases, this is called chainging.

  /* case class intHolder(int:Int){
    def flatMap =
      this.int
  }

  case class floatHolder(float:Float){
    def flatMap =
      this.float
  }

  case class doubleHolder(double:Double){
    def flatMap =
      this.double
  }

  def strToInt(num:String):intHolder =
    intHolder(num.toInt)


  def intToFloat(num:Int) =
    floatHolder(num.toFloat)


  def fltToDouble(num:Float) =
    doubleHolder(num)


  @main def run2() =
    val startNum:String = "1"
    val double = for
        int <- strToInt(startNum)
        float <- intToFloat(int)
        double <- fltToDouble(float)
    yield double
    println(double)
   */
}
