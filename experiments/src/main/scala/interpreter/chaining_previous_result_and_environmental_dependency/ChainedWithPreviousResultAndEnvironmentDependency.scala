package interpreter.chaining_previous_result_and_environmental_dependency

import environment_exploration.ToyEnvironment
import zio.{ZIO, ZIOAppDefault}

import scala.reflect.{ClassTag, classTag}
import scala.util.Random

trait Operation[
    Dependencies <: Service: ClassTag
]:
  val dep: ClassTag[Dependencies] =
    classTag[Dependencies]

case class Value(value: String)
    extends Operation[AnyService]

case class StringManipulation(
    action: String => String
) extends Operation[AnyService]:
  def actOn(input: String): String =
    action(input)

case class Print() extends Operation[Printer]

case class RandomString()
    extends Operation[RandomService]

trait Service
case class AnyService()    extends Service
case class RandomService() extends Service

val program =
  Seq(
    Value("Hello There"),
    Print(),
    StringManipulation(_.toUpperCase()),
    Print(),
    StringManipulation(_.take(5)),
    Print(),
    RandomString(),
    Print(),
    StringManipulation(_.toUpperCase()),
    Print()
  )

trait Printer extends Service:
  def print(input: String): Unit

def interpretWithEnvironment(
    program: Seq[Operation[_]],
    environment: ToyEnvironment[Service]
): String =
  program.foldLeft("") { (acc, op) =>
//    environment.get(op.dep)
    op match
      case Print() =>
//        environment.get[Printer].print(acc)
        acc
      case RandomString() =>
        scala
          .util
          .Random
          .alphanumeric
          .take(10)
          .mkString
      case Value(value) =>
        value
      case StringManipulation(action) =>
        action(acc)
  }
@main
def demoInterpreter() =

  val env =
    ToyEnvironment(Map.empty)
      .add(RandomService())
      .add(AnyService())
  interpretWithEnvironment(program, env)
