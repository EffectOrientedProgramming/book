package environment_exploration

import scala.reflect.{ClassTag, classTag}

case class DBService(url: String)

// Yada yada yada lets talk about the environment
trait ToyEnvironmentT[+R]:
  def add[A: ClassTag](
      a: A
  ): ToyEnvironmentT[R & A]
  def get[A >: R: ClassTag]: A

class ToyEnvironment[+R](
    typeMap: Map[ClassTag[_], _]
) extends ToyEnvironmentT[R]:
  def add[A: ClassTag](
      a: A
  ): ToyEnvironment[R & A] =
    ToyEnvironment(typeMap + (classTag[A] -> a))

  def get[A >: R: ClassTag]: A =
    typeMap(classTag[A]).asInstanceOf[A]

@main
def demoToyEnvironment =
  val env: ToyEnvironment[_] =
    ToyEnvironment(Map.empty)

  val env1: ToyEnvironment[String] =
    env.add("hi")

  val env2: ToyEnvironment[String & DBService] =
    env1.add(DBService("blah"))

  val env3: ToyEnvironment[
    String & DBService & List[String]
  ] = env2.add(List("a", "b"))

  println(env3.get[String])
  println(env3.get[DBService])
  println(env3.get[List[String]])

// We get some amount of compile time safety here, but not much
// println(env.get(classOf[List[DBService]]))
