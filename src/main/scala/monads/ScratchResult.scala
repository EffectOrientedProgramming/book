package monads
// Scratch/experimental example.
// This doesn't work because the Either is contained in Result via composition.
// You can't inherit from Either because it's sealed.
// I think the solution is to make my own minimal Result only containing map and flatMap.

object ScratchResult: // Keep from polluting the 'monads' namespace
  class Result[F, S](val either: Either[F, S])

  case class Fail[F](fail: F) extends Result(Left(fail))

  case class Succeed[S](s: S) extends Result(Right(s))

@main
def essence =
  println("The essence of a monad")