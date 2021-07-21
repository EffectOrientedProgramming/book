package visibility

private val packageOnly = 3

def packageStuff() = println(
  "doing package things!"
)

// Package-private
class Visibility:
  protected val x = 3
  val mine = "Mine!"

class VisibilitySub extends Visibility:
  val y = x + 3
//  val theirs = mine
