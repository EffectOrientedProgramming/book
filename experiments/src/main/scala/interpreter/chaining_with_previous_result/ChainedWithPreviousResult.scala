package interpreter.chaining_with_previous_result

import interpreter.level1_nochaining.Random
import zio.{ZIO, ZIOAppDefault}

trait Operation

case class Value(value: String) extends Operation

case class StringManipulation(
                  action: String => String
                ) extends Operation:
  def actOn(input: String): String = action(input)

object Print extends Operation

object RandomString extends Operation

val program = Seq(
  Value("Hello There"),
  Print,
  StringManipulation(_.toUpperCase().nn),
  Print,
  StringManipulation(_.take(5).nn),
  Print,
  RandomString,
  Print,
  StringManipulation(_.toUpperCase().nn),
  Print,
)

def interpret(program: Seq[Operation]): String =
  program.foldLeft("") { (acc, op) =>
    op match {
      case Print =>
        println(acc)
        acc
      case RandomString =>
        scala.util.Random.alphanumeric.take(10).mkString
      case Value(value) =>
        value
      case StringManipulation(action) =>
        action(acc)
    }
  }

@main
def demoInterpreter() = interpret(program)