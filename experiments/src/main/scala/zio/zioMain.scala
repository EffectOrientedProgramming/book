package zio

import scala.annotation.MainAnnotation.{Info, Parameter}
import scala.annotation.{MainAnnotation, experimental, nowarn}
import scala.util.CommandLineParser.FromString

@experimental
class zioMain extends MainAnnotation[FromString, ZIO[ZIOAppArgs, Any, Any]]:
  def argGetter[T](param: Parameter, arg: String, defaultArgument: Option[() => T])(using parser: FromString[T]): () => T =
    ???

  def command(info: Info, args: Seq[String]): Option[Seq[String]] =
    Some(Seq.empty)

  def run(program: () => ZIO[ZIOAppArgs, Any, Any]): Unit =
    ZIOAppDefault.fromZIO(program()).main(Array.empty)

  def varargGetter[T](param: Parameter, args: Seq[String])(using parser: FromString[T]): () => Seq[T] =
    ???
