package composability

import zio.*
import scala.concurrent.Future
import zio.direct.*

import scala.Option
import scala.util.{Success, Try}

// todo: turn into a relatable scenario
// todo: consider a multi-step build like in Superpowers

object AllTheThings extends ZIOAppDefault:
  type Nail = ZIO.type
  /*
    If ZIO is your hammer, it's not that you _see_ everything as nails.
    You can actually _convert_ everything into nails.
   */

  /*

    Possible scenario:
      Get headline - Future
      Analyze for topic/persons of interest - Option
      Lookup known information on them - Resource?
      Save event to DB  - Try

   Is Either different enough to demo here?
    It basically splits the difference between Option/Try
    I think if we show both of them, we can skip Either.

   */

  def getHeadline(): Future[String] =
    Future.successful("The stock market is crashing!")

  def asyncThing(i: Int) = ZIO.sleep(i.seconds)

  def errorThing[A](t: Try[A]) = ZIO.fromTry(t)

  val resourcefulThing
      : ZIO[Scope, Nothing, String] =
    val open =
      defer:
        Console.printLine("open").orDie.run
        "asdf"

    val close = (_: Any) => Console.printLine("close").orDie

    ZIO.acquireRelease(open)(close)

  override def run =
    defer:
      // todo: useful order, maybe async first or
      // near first?
      // maybe something parallel in here too?
      // Convert from AutoCloseable
      // maybe add Future or make asyncThing a
      // Future `
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

def futureBits = {
  ZIO.fromFuture(implicit ec =>
    Future.successful("Success!")
  )
  ZIO.fromFuture(implicit ec =>
    Future.failed(new Exception("Failure :("))
  )

}

