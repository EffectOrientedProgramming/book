package streams

import zio.*
import zio.stream.*
import zio.test.Gen

enum Code:
  case Ok,
    BadRequest,
    Forbidden

case class Path(segments: Seq[String]):
  override def toString: String =
    segments.mkString("/")

object Path:
  private val pathExamples =
    List(
      "login",
      "preferences",
      "settings"
    )

  private val userSections =
    List(
      "activity",
      "status",
      "collaborators"
    )

  private def randomElementFrom[T](collection: List[T]): ZIO[Any, Nothing, T] =
    for
      idx <- Random.nextIntBounded(collection.length)
    yield collection(idx)


  private val randomUser =
    for
      userId <- Random.nextIntBounded(1000)
      section <- randomElementFrom(userSections)
    yield s"/user/$userId/$section"

  val random =
    for
      userSection <- randomUser
//      numberOfSegments <-
//        Random.nextIntBetween(1, 4)
//      segments <-
//        ZIO.foreachPar(
//          List.fill(numberOfSegments)(())
//        )(_ => Random.nextString(8))
    yield Path(List(userSection))

  val randomGen =
    for
      numberOfSegments <-
        Random.nextIntBetween(1, 4)
      segments <-
        Gen
          .alphaNumericStringBounded(4, 8)
          .runCollectN(numberOfSegments)
//      segments <-
//        ZIO.foreachPar(List.fill(numberOfSegments)(()))(
//          _ => Random.nextString(8)
//        )
    yield Path(segments)
end Path

case class Request(response: Code, path: Path)

trait HttpRequestStream:
  def requests: Stream[Nothing, Request]

object HttpRequestStream:
  object Live extends HttpRequestStream:
    override def requests
        : Stream[Nothing, Request] =
      ZStream.repeatZIO(randomRequest)
        .schedule(Schedule.spaced(100.millis))

  private val randomRequest =
    for
      codeIndex <-
        Random.nextIntBounded(Code.values.length)
      path <- Path.random
    yield Request(
      Code.fromOrdinal(codeIndex),
      path
    )
