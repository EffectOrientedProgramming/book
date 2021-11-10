package effects

object mutableVars:
  // Mutable variables can be contisdered as
  // effects. They can change the behvior of a
  // function while not being an input.

  var x = 5

  def addXnY(y: Int) = x + y

  @main
  def mutableVarsEx =
    println(addXnY(3)) // This gives 8
    x = 2
    println(addXnY(3)) // This gives 5

// The calls to addXnY have the same inputs, yet
// give different outputs. This does
// not follow the rules of a pure function.
