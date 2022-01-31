package environmentexploration

import izumi.reflect.macrortti.LightTypeTag
import zio.IsNotIntersection
import izumi.reflect.Tag

object TypeMapEnvironment:
  val t: LightTypeTag = ???

class TypeMapEnvironment[+R](
    private val map: Map[LightTypeTag, Any]
):
  self =>
  def add[A](a: A)(implicit
      ev: IsNotIntersection[A],
      tagged: Tag[A]
  ): TypeMapEnvironment[R with A] =
    new TypeMapEnvironment(
      self.map + (taggedTagType(tagged) -> a)
    )

  def taggedTagType[A](
      tagged: Tag[A]
  ): LightTypeTag = tagged.tag
