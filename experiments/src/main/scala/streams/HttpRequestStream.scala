package streams

import zio.*
import zio.stream.*

enum Code:
  case Ok, BadRequest, Forbidden

case class Path(segments: Seq[String]):
  override def toString: String =
    segments.mkString("/")

object Path:
  val random =
    for
      numberOfSegments <- Random.nextIntBetween(1, 4)
      segments <-
        ZIO.foreachPar(List.fill(numberOfSegments)(()))(
          _ => Random.nextString(8)
        )
    yield Path(segments)

case class Request(response: Code, path: Path)

trait HttpRequestStream:
  def requests: Stream[Nothing, Request]

object HttpRequestStream:
  object Live extends HttpRequestStream:
    override def requests: Stream[Nothing, Request] =
      ZStream.repeatZIO(randomRequest)

  private val randomRequest =
    for
      codeIndex <- Random.nextIntBounded(Code.values.length)
      path <- Path.random
    yield Request(Code.fromOrdinal(codeIndex), path)


