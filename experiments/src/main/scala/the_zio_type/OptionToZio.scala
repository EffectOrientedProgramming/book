// OptionToZio.scala
package ScalaTypesToZio

import java.io
import zio._
import java.io.IOException

class OptionToZio:

  val sOption1: Option[Int] =
    Some(1) // sOption is either 1 or None

  // Here we convert the Scala Options to ZIOs
  // using .fromOption()
  val zOption1: IO[Option[Nothing], Int] =
    ZIO.fromOption(sOption1)

  case class Person(name: String)
  val person1                  = Person("Bob")
  val sOption2: Option[Person] = Some(person1)

  val zOption2: IO[Option[Nothing], Person] =
    ZIO.fromOption(sOption2)
