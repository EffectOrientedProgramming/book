package prelude

import zio.prelude.Assertion
import zio.prelude.Assertion.*

case class OurPrimitiveClass(
    id: String,
    age: Int
):
  assert(age > 0)

object OurPrimitiveClass:
  def safeConstructor(
      id: String,
      age: Int
  ): Either[String, OurPrimitiveClass] =
    if (age > 0)
      Right(OurPrimitiveClass(id, age))
    else
      Left("Invalid age")

  implicit val ordering
      : Ordering[OurPrimitiveClass] =
    new Ordering[OurPrimitiveClass]:
      def compare(
          x: OurPrimitiveClass,
          y: OurPrimitiveClass
      ): Int = x.age.compare(y.age)
