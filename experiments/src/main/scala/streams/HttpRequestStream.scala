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
  private val genericPaths =
    List(
      "login",
      "preferences",
      "settings",
      "home",
      "latest",
      "logout"
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

  private val randomGeneric: ZIO[Any, Nothing, String] =
    for
      section <- randomElementFrom(genericPaths)
    yield s"/$section"

  private val randomUser: ZIO[Any, Nothing, String] =
    for
      userId <- Random.nextIntBounded(1000)
      section <- randomElementFrom(userSections)
    yield s"/user/$userId/$section"

  private val generators =
    List(
      randomGeneric,
      randomUser
    )

  val random =
    for
      generator <- randomElementFrom(generators)
      path <- generator
//      numberOfSegments <-
//        Random.nextIntBetween(1, 4)
//      segments <-
//        ZIO.foreachPar(
//          List.fill(numberOfSegments)(())
//        )(_ => Random.nextString(8))
    yield Path(List(path))

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
