package scalaBasics

object flatMap {
//In it's most basic sense, flatMap is the combination of
// the two functions map(), and flatten().
  /*

  @main def flatMapEx =
    val words = Seq("Hello", "World", "Of", "Scala!")

    val flattenedWords = words.flatten
    val upperFlattenedWords = flattenedWords.map(_.toUpper)
    println(upperFlattenedWords)

    val flatMapped = words.flatMap(_.toUpperCase)
    println(flatMapped)
   */

  /*
//In Funcitonal Programming, flatMap can be used in error handling.
// Flat Map can behave like a Map() that can fail.
  //For example, when using Options:

  def map[B](f: A => B):Option[B] =
    this match {
      case None => None      //If the object calling map is None, return None
      case Some(a) => Some(f(a)) //If the object calling map is something, return something holding f(something)
    }
  //Map calls f for each of it's item, then returns an Option for the whole list

  def flatMap[B](f: A => Option[B]):Option[B]
    this match {
      case None => None  //If the object calling map is None, return None
      case Some(a) => f(a) //If the object calling map is something, return f(something)
    }
  //flatMap calls a function f that returns an Option
  //for each of the items. Then it reaturns the trasformed list as an option.

   */

}
