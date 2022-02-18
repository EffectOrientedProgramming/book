package interpreter.chaining_with_previous_result

import zio.{ZIO, ZIOAppDefault}

trait Operation:
  def actOn(input: String): String

case class Value(value: String) extends Operation:
  def actOn(input: String): String = value

case class StringManipulation(
                  action: String => String
                ) extends Operation:
  def actOn(input: String): String = action(input)

object Print extends Operation:
  def actOn(input: String): String =
    println(input)
    input


val program = Seq(
  Value("Hello There"),
  Print,
  StringManipulation(_.toUpperCase().nn),
  Print,
  StringManipulation(_.take(5).nn),
  Print
)

def interpret(program: Seq[Operation]): String =
  program.foldLeft("") { (acc, op) =>
    op.actOn(acc)
  }

@main
def demoInterpreter() = interpret(program)