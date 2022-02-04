package environmentexploration

case class DBService(url: String)

// Yada yada yada lets talk about the environment
trait ToyEnvironmentT[+R]:
  def add[A](a: A): ToyEnvironment[R & A]
  def get[A >: R](classA: Class[A]): A

class ToyEnvironment[+R](
    typeMap: Map[Class[_], Any]
) extends ToyEnvironmentT[R]:
  def add[A](a: A): ToyEnvironment[R & A] =
    ToyEnvironment(typeMap + (a.getClass -> a))

  def get[A >: R](classA: Class[A]): A =
    typeMap(classA).asInstanceOf[A]

@main
def demoTypeMapInsertionAndRetrieval =
  val env: ToyEnvironment[Any] =
    ToyEnvironment[Any](Map.empty)

  val env1: ToyEnvironment[Any & String] =
    env.add("hi")

  val env2: ToyEnvironment[
    Any & String & DBService
  ] = env1.add(DBService("blah"))

  val env3: ToyEnvironment[
    Any & String & DBService & List[String]
  ] = env2.add(List("a", "b"))

  println(env3.get(classOf[String]))
  println(env3.get(classOf[DBService]))
  // Blows up at runtime, because we can't
  // actually cast generic types to concrete
  // types
  println(env3.get(classOf[List[String]]))

// We get some amount of compile time safety here, but not much
// println(env.get(classOf[List[DBService]]))
end demoTypeMapInsertionAndRetrieval
