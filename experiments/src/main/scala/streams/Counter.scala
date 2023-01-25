package streams

import zio.{Ref, ZIO}

case class Counter(count: Ref[Int]):
  def get: ZIO[Any, Nothing, Int] =
    count.getAndUpdate(_ + 1)

object Counter:
  val make = Ref.make(0).map(Counter(_))
