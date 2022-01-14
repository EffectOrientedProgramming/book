package ZIOFromNothing

class XEnvironment():
  def increment(y: Int): Int =
    XEnvironment.x += y
    XEnvironment.x

object XEnvironment:
  private var x: Int = 0
