package environmentexploration

import izumi.reflect.macrortti.LightTypeTag
import zio.{IsNotIntersection, ZIO, ZIOAppDefault}
import izumi.reflect.Tag

import scala.reflect.ClassTag

case class OurDomainType(value: String)

case class DumbTypeTag( value: String )

def typeTagFor(a: Any): DumbTypeTag =
  DumbTypeTag(a.getClass.toString)


object DemoTypeMap extends ZIOAppDefault:
  def run =
    ZIO.debug(typeTagFor("hi")) *> ZIO.debug(typeTagFor(3)) *> ZIO.debug(typeTagFor(OurDomainType("blah")))


import zio.ZEnvironment

case class TypeMapEnvironment[+R](
    private val map: Map[Class[_], Any]
):
    def add[A](a: A): TypeMapEnvironment[R with A] =
      new TypeMapEnvironment(
        map + (a.getClass -> a)
      )

    def taggedTagType[A](
        tagged: Tag[A]
    ): LightTypeTag = tagged.tag

    def get[A >: R](classA: Class[A]): A =
      map.get(classA).map(_.asInstanceOf[A]).get

object DemoTypeMapInsertionAndRetrieval extends ZIOAppDefault:
  def run =
      ZIO {
        val env = new TypeMapEnvironment[Any](Map.empty)
        val input: String = "hi"
        val env1 = env.add(input)
        val env2 = env1.add(OurDomainType("blah"))
        println(env2.get(classOf[String]))
        println(env2.get(classOf[OurDomainType]))
      }
