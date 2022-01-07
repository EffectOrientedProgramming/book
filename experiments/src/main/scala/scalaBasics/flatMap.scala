package scalaBasics

import scala.util.*

object flatMap {
  // In its most basic sense, flatMap is the
  // combination of
  // the two functions map(), and flatten().

  /* @main def flatMapEx =
   * val nums =
   * Seq(List(1,2,3),List(1,2,3),List(1,2,3))
   *
   * def addOne(num:Int): Int =
   * num + 1
   *
   * println("\nFlat, then map: ") val flatWords
   * = nums.flatten println(flatWords) val
   * mappedFlatWords = flatWords.map(addOne)
   * println(mappedFlatWords)
   *
   * println("\nflatMap: ") val flatMapped =
   * nums.flatMap(addOne) println(flatMapped) */
  /* //In Functional Programming, flatMap can be
   * used in error handling.
   * // Flat Map can behave like a Map() that can
   * fail.
   * //For example, when using Options:
   *
   * def map[B](f: A => B):Option[B] =
   * this match { case None => None //If the
   * object calling map is None, return None case
   * Some(a) => Some(f(a)) //If the object
   * calling map is something, return something
   * holding f(something) } //Map calls f for
   * each of it's item, then returns an Option
   * for the whole list
   *
   * def flatMap[B](f: A => Option[B]):Option[B]
   * this match { case None => None //If the
   * object calling map is None, return None case
   * Some(a) => f(a) //If the object calling map
   * is something, return f(something) }
   * //flatMap calls a function f that returns an
   * Option //for each of the items. Then it
   * returns the transformed list as an option. */

}
