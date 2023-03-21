## std_type_conversions_to_zio

 

### experiments/src/main/scala/std_type_conversions_to_zio/EitherToZio.scala
```scala
// EitherToZio.scala
package std_type_conversions_to_zio

import zio.{ZIO, ZIOAppDefault}

import scala.util.{Left, Right}

case class InvalidIntegerInput(value: String)

object EitherToZio extends ZIOAppDefault:
  val goodInt: Either[InvalidIntegerInput, Int] =
    Right(42)

  val zEither
      : ZIO[Any, InvalidIntegerInput, Int] =
    ZIO.fromEither(goodInt)

  def run = zEither.debug("Converted Either")

```


### experiments/src/main/scala/std_type_conversions_to_zio/FutureToZio.scala
```scala
package std_type_conversions_to_zio

import zio.{ZIO, ZIOAppDefault}
import scala.concurrent.Future

object FutureToZio extends ZIOAppDefault:

  val zFuture =
    ZIO.fromFuture(implicit ec =>
      Future.successful("Success!")
    )

  val zFutureFailed =
    ZIO.fromFuture(implicit ec =>
      Future.failed(new Exception("Failure :("))
    )

  val run =
    zFutureFailed.debug("Converted Future")

```


### experiments/src/main/scala/std_type_conversions_to_zio/OptionToZio.scala
```scala
package std_type_conversions_to_zio

import java.io
import zio._
import java.io.IOException

class OptionToZio extends ZIOAppDefault:

  val alias: Option[String] =
    Some("Buddy") // sOption is either 1 or None

  val aliasZ: IO[Option[Nothing], String] =
    ZIO.fromOption(alias)

  val run = aliasZ

```


### experiments/src/main/scala/std_type_conversions_to_zio/TryToZio.scala
```scala
package std_type_conversions_to_zio

import zio._
import java.io
import java.io.IOException
import scala.util.Try

object TryToZio extends ZIOAppDefault:
  val dividend = 42
  val divisor  = 7

  // Significant Note: Try is a standard
  // collection by-name function. This makes
  // it a good candidate for introducting that
  // concept.
  def sTry: Try[Int] = Try(dividend / divisor)

  val zTry: IO[Throwable, Int] =
    ZIO.fromTry(sTry)

  val run = zTry

```


