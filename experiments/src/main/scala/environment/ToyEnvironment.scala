package environment

import scala.reflect.{ClassTag, classTag}

case class DBService(url: String)

// Yada yada yada lets talk about the environment
trait ToyEnvironmentT[+R]:

  def add[A: ClassTag](
      a: A
  ): ToyEnvironmentT[R & A]

  def get[A >: R: ClassTag]: A

class ToyEnvironment[+R](
    typeMap: Map[ClassTag[_], Any]
) extends ToyEnvironmentT[R]:

  def add[A: ClassTag](
      a: A
  ): ToyEnvironment[R & A] =
    ToyEnvironment(typeMap + (classTag[A] -> a))

  def get[A >: R: ClassTag]: A =
    typeMap(classTag[A]).asInstanceOf[A]

@annotation.nowarn
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

  // We get some amount of compile time safety
  // here, but not much
  // println(env.get(classOf[List[DBService]]))

  // Downside of the current approach is that it
  // doesn't prevent duplicate types
  env3.add("hi") // is accepted
end demoToyEnvironment

// Consider this runtime de-duping

class ToyEnvironmentRuntimeDeduplication[+R](
    typeMap: Map[ClassTag[_], Any]
):

  def add[A: ClassTag](
      a: A
  ): ToyEnvironment[R & A] =
    if (typeMap.contains(classTag[A]))
      throw new IllegalArgumentException(
        s"Cannot add ${classTag[A]} to environment, it already exists"
      )
    else
      ToyEnvironment(
        typeMap + (classTag[A] -> a)
      )
