package mdoc

import zio.Runtime.default.unsafeRun
import zio.{ZIO, ZEnv, Console}

def unsafeRunTruncate[E, A](
    z: => ZIO[zio.ZEnv, E, A]
): A | Unit =
  val res: ZIO[zio.ZEnv, E, A | Unit] =
    z.catchAllDefect { case defect: Any =>
      ZIO.debug(
        s"Unhandled defect: $defect".take(47)
      )
    }
  unsafeRun(res)

/** final def unsafeRun[E, A]( zio: => ZIO[R, E,
  * A] ): A = unsafeRunSync(zio) .getOrElse(c =>
  * throw FiberFailure(c))
  */
