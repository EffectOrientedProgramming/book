package environment

import scala.reflect.{ClassTag, classTag}

trait DBService

trait ToyEnvironmentT[+R]:

  def add[A: ClassTag](
      a: A
  ): ToyEnvironmentT[R & A]

  def get[A >: R: ClassTag]: A

class ToyEnvironment[+R](
    typeMap: Map[ClassTag[_], Any]
) extends ToyEnvironmentT[R]:

  // doesn't prevent duplicate types
  def add[A: ClassTag](
      a: A
  ): ToyEnvironment[R & A] =
    ToyEnvironment(typeMap + (classTag[A] -> a))

  def get[A >: R: ClassTag]: A =
    typeMap(classTag[A]).asInstanceOf[A]

@annotation.nowarn
@main
def demoToyEnvironment =
  val env: ToyEnvironment[Unit] =
    ToyEnvironment(Map.empty)

  // mdoc:fails
  // env.get[String]

  val env1: ToyEnvironment[String] =
    env.add("hi")

  val env2: ToyEnvironment[String & DBService] =
    env1.add(new DBService{})

  println(env2.get[String])
  println(env2.get[DBService])


end demoToyEnvironment
