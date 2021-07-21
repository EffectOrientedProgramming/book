package typeclasses.equality

/** We are showing the ability to add behavior
  * to classes we don't control. Typeclasses:
  * the generic form of extension methods?
  */
trait WhatEquals[T]:
  def strEq(s: String, t: T): Boolean

given WhatEquals[Boolean] with

  def strEq(s: String, t: Boolean): Boolean =
    (s == "true" && t == true) || (s == "false" && t == false)

// Note: This syntax gives garbage error message when a TC is not available
// "the result of an implicit conversion must be more specific than Object"
//extension [T](s: String)(using we: WhatEquals[T])
//  def eq(t: T): Boolean = we.strEq(s, t)

extension [T: WhatEquals](s: String)

  def eq(t: T): Boolean =
    summon[WhatEquals[T]].strEq(s, t)

@main def eqStuff =
  println("hi".eq(true))
  println("true".eq(true))
  println("false".eq(false))

//  println("false".eq(10))
