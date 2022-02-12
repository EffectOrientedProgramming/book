package environment_exploration.opaque

import scala.reflect.{ClassTag, classTag}

case class DBService(url: String)

opaque type ToyEnvironment[R] =
  Map[ClassTag[_], _]

object ToyEnvironment:
  def apply[A: ClassTag](
      a: A
  ): ToyEnvironment[A] = Map(classTag[A] -> a)

extension [R](env: ToyEnvironment[R])
  def add[A: ClassTag](
      a: A
  ): ToyEnvironment[R & A] =
    env + (classTag[A] -> a)
  def get[A >: R: ClassTag]: A =
    env(classTag[A]).asInstanceOf[A]

@main
def demoToyEnvironment =

  val env1: ToyEnvironment[String] =
    ToyEnvironment("hi")

  val env2: ToyEnvironment[String & DBService] =
    env1.add(DBService("blah"))

  val env3: ToyEnvironment[
    String & DBService & List[String] & List[Int]
  ] = env2.add(List("a", "b")).add(List(1, 2))

  println(env3.get[String])
  println(env3.get[DBService])
  println(env3.get[List[String]])
  println(env3.get[List[Int]])

// We get some amount of compile time safety here, but not much
// println(env.get(classOf[List[DBService]]))
