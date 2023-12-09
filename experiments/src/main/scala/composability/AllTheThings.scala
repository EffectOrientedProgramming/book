package composability

import zio.*
import zio.direct.*

import scala.Option
import scala.util.{Success, Try}

// todo: turn into a relatable scenario
// todo: consider a multi-step build like in Superpowers
object AllTheThings extends ZIOAppDefault:

  def asyncThing(i: Int) = ZIO.sleep(i.seconds)

  def optionThing(o: Option[Int]) =
    ZIO.fromOption(o)

  def errorThing[A](t: Try[A]) = ZIO.fromTry(t)

  val resourcefulThing
      : ZIO[Scope, Nothing, String] =
    val open =
      defer:
        Console.printLine("open").orDie.run
        "asdf"

    val close = Console.printLine("close").orDie

    ZIO.acquireRelease(open)(_ => close)

  override def run =
    defer:
      // todo: useful order, maybe async first or
      // near first?
      // maybe something parallel in here too?
      // maybe add Future or make asyncThing a
      // Future
      val s: String = resourcefulThing.run
      val t: Try[String] =
        Success(
          s
        ) // todo: some failable function
      val w: String = ZIO.fromTry(t).run
      val o: Option[Int] =
        Option.unless(w.isEmpty)(
          w.length
        ) // todo: some optional function
      val i: Int = ZIO.fromOption(o).debug.run
      asyncThing(i).run
      // todo: some error handling to show that
      // the errors weren't lost along the way
    .catchAll:
      case t: Throwable =>
        ???
      case _: Any =>
        ???
end AllTheThings
