package composability

import zio.*

import scala.concurrent.Future
import zio.direct.*

import java.lang.AutoCloseable
import scala.Option
import scala.util.{Success, Try}

// todo: turn into a relatable scenario
// todo: consider a multi-step build like in Superpowers

object AllTheThings extends ZIOAppDefault:
  type Nail = ZIO.type
  /* If ZIO is your hammer, it's not that you
   * _see_ everything as nails.
   * You can actually _convert_ everything into
   * nails. */

  /* Possible scenario:
   * Get headline - Future Analyze for
   * topic/persons of interest - Option Check if
   * we have made an entry for them in today's
   * summary file - Resource If not:
   * Dig up supporting information on the topic
   * from a DB - Try Make new entry in today's
   * summary file - Resource
   *
   * Is Either different enough to demo here?
   * It basically splits the difference between
   * Option/Try I think if we show both of them,
   * we can skip Either. */

  def getHeadline(): Future[String] =
    Future.successful(
      "The stock market is crashing!"
    )

  def findTopicOfInterest(
      content: String
  ): Option[String] =
    Option
      .when(content.contains("stock market")):
        "stock market"

  trait CloseableFile extends AutoCloseable:
    def existsInFile(searchTerm: String): Boolean

    def close: Unit
    def write(entry: String): Unit

  val summaryFile: CloseableFile =
    new CloseableFile:
      override def close =
        println("Closing file now!")

      override def existsInFile(
          searchTerm: String
      ): Boolean = searchTerm == "stock market"

      override def write(entry: String) = ???

  def asyncThing(i: Int) = ZIO.sleep(i.seconds)

  val resourcefulThing =
    val open =
      defer:
        Console.printLine("open").orDie.run
        "asdf"

    val close =
      (_: Any) =>
        Console.printLine("close").orDie

    ZIO.acquireRelease(open)(close)

  override def run =
    defer:
      // todo: useful order, maybe async first or
      // near first?
      // maybe something parallel in here too?
      // Convert from AutoCloseable
      // maybe add Future or make asyncThing a
      // Future `
      val headline: String =
        ZIO
          .from:
            getHeadline()
          .run

      val topic =
        ZIO
          .from:
            findTopicOfInterest(headline)
          .run

      val summaryFileZ =
        ZIO
          .fromAutoCloseable:
            ZIO.succeed:
              summaryFile
          .run

      val t: Try[String] = Success(headline)
      // todo: some failable function
      val w: String = ZIO.from(t).run
      val o: Option[Int] =
        Option.unless(w.isEmpty):
          w.length
      val i: Int = ZIO.from(o).debug.run
      asyncThing(i).run
      // todo: some error handling to show that
      // the errors weren't lost along the way
    .catchAll:
      case t: Throwable =>
        ???
      case _: Any =>
        ???
end AllTheThings

def futureBits =
  ZIO.fromFuture(implicit ec =>
    Future.successful("Success!")
  )
  ZIO.fromFuture(implicit ec =>
    Future.failed(new Exception("Failure :("))
  )
