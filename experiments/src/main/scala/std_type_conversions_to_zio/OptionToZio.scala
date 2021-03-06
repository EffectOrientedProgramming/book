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
