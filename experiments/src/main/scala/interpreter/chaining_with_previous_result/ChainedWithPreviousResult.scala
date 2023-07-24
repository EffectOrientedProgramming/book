package interpreter.chaining_with_previous_result

import environment_exploration.ToyEnvironment

import scala.reflect.ClassTag
import scala.util.Random

trait Operation

case class Value(value: String) extends Operation

case class StringManipulation(
    action: String => String
) extends Operation:
  def actOn(input: String): String =
    action(input)

case class Print() extends Operation

case class RandomString() extends Operation

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

def interpret(program: Seq[Operation]): String =
  program.foldLeft("") { (acc, op) =>
    op match
      case Print() =>
        println(acc)
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

@annotation.nowarn
@main
def demoInterpreter() = interpret(program)

trait Printer:
  def print(input: String): Unit

def interpretWithEnvironment(
    program: Seq[Operation],
    environment: ToyEnvironment[Printer & Random]
): String =
  program.foldLeft("") { (acc, op) =>
    op match
      case Print() =>
        environment.get[Printer].print(acc)
        acc
      case RandomString() =>
        environment
          .get[Random]
          .alphanumeric
          .take(10)
          .mkString
      case Value(value) =>
        value
      case StringManipulation(action) =>
        action(acc)
  }
