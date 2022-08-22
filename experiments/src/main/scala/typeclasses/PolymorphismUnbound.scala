package typeclasses

import zio.*

class Dog():
  def bark() = println("woof")

class Person():
  def greet() = println("hello")


trait Communicate[T]:
  extension (t:T) def communicate(): Unit



given Communicate[Person] with
  extension (t: Person)
    override def communicate(): Unit = t.greet()

given Communicate[Dog] with
  extension (t: Dog)
    override def communicate(): Unit = t.bark()

object PolymorphismUnbound extends App:

  def demo[T](instance: T)(using Communicate[T]) =
    instance.communicate()

  demo(
//        Person()
    Dog()
  )
