package zioBasics

import java.io
import zio._
import java.io.IOException

object Alias:
  // General Alias Table:
  // UIO[A] = ZIO[Any, Nothing, A] Any
  // environment, No Errors
  // URIO[R,A] = ZIO[R, Nothing, A]    No errors
  // Task[A] = ZIO[Any, Throwable, A] Any
  // environment, Throwable errors
  // RIO[A] = ZIO[R, Throwable, A] Throwable
  // Errors
  // IO[E,A] = ZIO[Any, E, A] Any Environment

  // suc(1-5) are all equivalant.
  // Their types can be aliased to be more and
  // more specific.
  val suc1 = ZIO.succeed(1)

  val suc2: ZIO[Any, Nothing, Int] =
    ZIO.succeed(1)
  val suc3: IO[Nothing, Int] = ZIO.succeed(1)
  val suc4: URIO[Any, Int]   = ZIO.succeed(1)
  val suc5: UIO[Int]         = ZIO.succeed(1)
end Alias
