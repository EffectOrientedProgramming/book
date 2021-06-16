package handlingErrors

import zio.*

object value {
  //Either and Absolve take ZIO types and 'surface' or 'submerge'
  //the error.

  //Either takes an ZIO[R, E, A] and produces an ZIO[R, Nothing, Either[E,A]]
  //The error is 'surfaced' by making a non-failing ZIO that returns an Either.

  //Absolve takes an ZIO[R, Nothing, Either[E,A]], and returns a ZIO[R,E,A]
  //The error is 'submerged', as it is pushed from an either into a ZIO.

  val zeither: UIO[Either[String, Int]] =
    IO.fail("Boom").either

  //IO.fail("Boom") is naturlally type ZIO[R,String,Int], but is
  //converted into type UIO[Either[String, Int]

  def sqrt(input: UIO[Double]): IO[String, Double] =
    ZIO.absolve(
      input.map(value =>
        if (value < 0.0) Left("Value must be >= 0.0")
        else Right(Math.sqrt(value))
        )
    )

  //The Left-Right statements naturally from an 'either' of type either[String, Double].
  //the ZIO.absolve changes the either into an ZIO of type IO[String, Double]

}
