package interpreter.chaining_with_monads

import scala.annotation.tailrec

sealed trait Operation:
  def map(mf: String => String): MapOp
  def flatMap(mf: String => Operation): FlatMapOp

class Value(val s: String) extends Operation:
  override def map(mf: String => String): MapOp =
    MapOp(_ => mf(s))
  override def flatMap(
      mf: String => Operation
  ): FlatMapOp = FlatMapOp(_ => mf(s))

class MapOp(val f: String => String)
    extends Operation:
  override def map(mf: String => String): MapOp =
    MapOp(f.andThen(mf))
  override def flatMap(
      mf: String => Operation
  ): FlatMapOp = FlatMapOp(f.andThen(mf))

class FlatMapOp(val f: String => Operation)
    extends Operation:
  override def map(mf: String => String): MapOp =
    MapOp(identity).flatMap(f).map(mf)

  override def flatMap(
      mf: String => Operation
  ): FlatMapOp =
    FlatMapOp { s =>
      f(s).flatMap(mf)
    }

@tailrec
def interpret(program: Operation): String =
  program match
    case v: Value =>
      println("run Value")
      v.s

    case mo: MapOp =>
      println("run MapOp")
      mo.f("")

    case fmo: FlatMapOp =>
      println("run FlatMapOp")
      val next = fmo.f("")
      interpret(next)

@main
def demoInterpreter() =
  val value = Value("asdf")
  println(interpret(value) == "asdf")

  val upperOp = MapOp(_.toUpperCase.nn)
  println(
    interpret(upperOp) == ""
  ) // applies the operation to the default empty string

  val valueUpperOp = value.map(_.toUpperCase.nn)
  println(interpret(valueUpperOp) == "ASDF")

  val valueUpperTakeTwoOp =
    valueUpperOp.map(_.take(2))
  println(interpret(valueUpperTakeTwoOp) == "AS")

  val flatMapOpToValue =
    FlatMapOp(_ => Value("asdf"))
  println(interpret(flatMapOpToValue) == "asdf")

  val flatMapOpToFlatMapOpToValue =
    FlatMapOp(_ => FlatMapOp(_ => Value("asdf")))
  println(
    interpret(flatMapOpToFlatMapOpToValue) ==
      "asdf"
  )

  val flatMapOpToMapOp =
    FlatMapOp(_ => MapOp(_ => "asdf"))
  println(interpret(flatMapOpToMapOp) == "asdf")

  val valueFlatMapOpToValue =
    value.flatMap(Value(_))
  println(
    interpret(valueFlatMapOpToValue) == "asdf"
  )

  val valueFlatMapOpToValueMapOp =
    value.flatMap(asdf =>
      Value(asdf).map(_.toUpperCase.nn)
    )
  println(
    interpret(valueFlatMapOpToValueMapOp) ==
      "ASDF"
  )

  val valueFlatMapOpToValueToFlatMap =
    value
      .flatMap(asdf =>
        Value(asdf.toUpperCase.nn)
      )
      .flatMap(upper => Value(upper.take(2)))
  println(
    interpret(valueFlatMapOpToValueToFlatMap) ==
      "AS"
  )

  val program =
    Value("asdf").flatMap { asdf =>
      println(s"asdf = $asdf")
      Value(asdf.toUpperCase.nn).map { upper =>
        println(s"upper = $upper")
        upper.take(2)
      }
    }

  println(interpret(program))

  val programFor =
    for
      asdf  <- Value("asdf")
      upper <- Value(asdf.toUpperCase.nn)
    yield upper.take(2)

  println(interpret(programFor))
end demoInterpreter
