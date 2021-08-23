package effects

import scala.util.Random

object randomComponents:

// Anthing that has a randomly generated
  // component is an effect

  def randNum: Unit =
    val rand = Random.nextInt(100)
    println(rand)

  @main
  def randNumEx =
    randNum
    randNum
// These have the same input, yet different
// outputs.
