package scalaBasics

object tailEndRecursion:

// The Scala compiler will be able to optimize
  // a recursive structure into byte code similar
// to a while loop if the recursive structure
  // is a 'tail end' recursion.

  def tailEndEx(num: Int): Int =
    @annotation.tailrec
    def fib(n: Int, a: Int, b: Int): Int =
      if (n == 0)
        a
      else if (n == 1)
        b
      else
        fib(n - 1, b, a + b)
    fib(num, 0, 1)

  @main
  def fib6 =
    val fib6 = tailEndEx(6) // Expected output: 8
    println(fib6)
end tailEndRecursion
