package scalaBasics

// This example will go over how to use the @main
// notation

object usingMains:

  // Within a package, the compiler will analyze
  // all of the executable
  // functions, and select one of them as the
  // 'main function'.
  // Something useful that scala 3 does is to
  // give the programmer the
  // ability to define multiple main functions
  // even within the same file.

  @main
  def main1() = println("I am main function 1!")

  @main
  def main2() = println("I am main function 2!")

  // If a function has the @main tag before its
  // definition,
  // sbt will recognize it as a main function,
  // and it will be seen
  // as runnable.

  def foo(input: String) = print(input)

  def bar = print("llo")

  def f = println(" there!")

  @main
  def message =
    val str = "He"
    foo(str)
    bar
    f
end usingMains
