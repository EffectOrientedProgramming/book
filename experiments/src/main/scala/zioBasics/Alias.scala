package zioBasics

import zio._

object Alias:

  // suc(1-5) are all equivalant.
  // Their types can be aliased to be more and
  // more specific.
  val suc1 = ZIO.succeed(1)

  val suc2: ZIO[Any, Nothing, Int] =
    ZIO.succeed(1)
  val suc3: IO[Nothing, Int] = ZIO.succeed(1)
  val suc4: URIO[Any, Int]   = ZIO.succeed(1)
  val suc5: UIO[Int]         = ZIO.succeed(1)
