package scalaBasics

object forComprehension:

  // This example goes through the basics of the
  // for comprehension.
  // Intitially, it will be compared to the for
  // loop equivelant.

  val numbers = Vector(1, 2, 3, 4, 5, 6)

  // for loop
  def forloopEx =
    println("For Loop: ")
    for (i <- Range(0, 6)) {
      val value = numbers(i)
      print(s"$value, ")
    }
    println()

  // for comprehension
  def forCompEx =
    println("For Comprehension: ")
    for {
      i <- numbers
    } print(s"$i" + ", ")
    println()

  // Example function: This function is slightly
  // more complex.
  // It demonstrates how to filter elements in a
  // for comprehension.
  def evenGT5v1(v: Vector[Int]): Vector[Int] =
    // 'var' so we can reassign 'result':
    println(
      "Finding values greater than 5 and even: "
    )
    var result = Vector[Int]()
    for {
      n <-
        v //Take the input value v, and itterate through each element
      if n > 5 //If n is greater than 5
      if n % 2 == 0 //and n is divisible by 2
    } result =
      result :+ n //Then add n to the result list
    result        //return result
  end evenGT5v1

// To remove the use of a var, and simplify the
  // code, you can use the yield keyword.
  // 'Yield'ing will create a list of all the
  // values that satasfied the critria.
  // 'Yield'ing essentially creates the list in
  // place.
  def evenGT5v2(v: Vector[Int]): Vector[Int] =
    // 'var' so we can reassign 'result':
    println(
      "Finding values greater than 5 and even: "
    )
    for
      n <-
        v //Take the input value v, and itterate through each element
      if n > 5 //If n is greater than 5
      if n % 2 == 0 //and n is divisible by 2
    yield n //create a list of the values of n.
  end evenGT5v2

  @main
  def run() =
    forloopEx
    forCompEx

    val v =
      Vector(1, 2, 3, 5, 6, 7, 8, 10, 13, 14, 17)
    println(evenGT5v1(v))
    println(evenGT5v2(v))
  end run

  // For comprehensions can also be used to
  // string together multiple events.
  // In some cases, this is called chainging. A
  // programmer would use a for comprehension
  // as it more clearly shows the sequentialsim
  // of a chain.

  // At this level, the arrow '<-" is called the
  // generator. The generator acts as either a
  // flatmap function,
  // or a map function. When using the 'yield'
  // functionality, the last '<-' represents a
  // map function instead of a
  // flatmap.

  // Here is a side by side example of a series
  // of function when called inside a for
  // comprehension vs not in a for comprehension:

  case class color(name: String):

    def flatMap(f: String => color): color =
      f(this.name)

    def map(f: String => String): color =
      color(f(this.name))

  object color:

    def makeRed: color = new color("red")

    def changeColor(name2: String): color =
      new color(name2)

    def changeColor2(name2: String): color =
      new color(name2)
  end color

// //////////////////////////////////////////////////////
  @main
  def thefor =
    val colorChanges =
      for
        color1 <- color.makeRed
        color2 <- color.changeColor("green")
        color3 <- color.changeColor2("yellow")
      yield color3
    println(colorChanges)

  @main
  def unraveled =
    val colorChanges =
      color
        .makeRed
        .flatMap { color1 => //color1 is a string
          color
            .changeColor("green")
            .flatMap {
              color2 => //color2 is a string
                color
                  .changeColor2("yellow")
                  .map {
                    color3 => //color3 is a color
                      color3
                  }
            }
        }
    println(colorChanges)
  end unraveled
end forComprehension

/* kat2 <- again(kat1) kat3 <- again(kat2) yield
 * kat3 // what we return here has to be a
 * Contents because it must fit in the Box */

// println(colorChanges)

/* @main def unravelled =
 * val kats = Box.observe.flatMap { kat1 =>
 * again(kat1).flatMap { kat2 => again(kat2).map
 * { kat3 => kat3 } } } */
