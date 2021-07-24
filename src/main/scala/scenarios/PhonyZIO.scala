package atomic

case class Schedule()

trait ZIO[R, E, A]:
  def map[B](f: A => B): ZIO[R, E, B] = ???

  def flatMap[R2, E2, B](
      f: A => ZIO[R2, E2, B]
  ): ZIO[R, E, B] = ???

  def retry(schedule: Schedule): ZIO[R, E, A] =
    ???

  def catchAll(
      handler: (E => A)
  ): ZIO[R, Nothing, A] = ???
end ZIO

case class UIO[A]() extends ZIO[Any, Nothing, A]

case class URIO[R, A]()
    extends ZIO[R, Nothing, A]

case class Task[A]()
    extends ZIO[Any, Throwable, A]

case class RIO[R, A]()
    extends ZIO[R, Throwable, A]

case class IO[E <: Throwable, A]()
    extends ZIO[Any, E, A]

object ZIO:

  def apply[T](
      body: => T
  ): ZIO[Any, Nothing, T] = ???

trait Has[A]
