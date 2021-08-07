package monads
// Just trying to understand what a type
// constructor is

// trait TypeConstructor[F[_], A]:
//  def use(tc: F): F[A]

trait Process[F[_], O]:
  def runLog(implicit
      F: MonadCatch[F]
  ): F[IndexedSeq[O]]

trait MonadCatch[F[_]]:
  def attempt[A](
      a: F[A]
  ): F[Either[Throwable, A]]
  def fail[A](t: Throwable): F[A]
