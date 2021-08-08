package mdoc

import zio.Runtime.default.unsafeRun
import zio.{ZIO, ZEnv}

def unsafeRunTruncate[E, A](
    z: => ZIO[zio.ZEnv, E, A]
): A | String =
  val res: ZIO[zio.ZEnv, E, A | String] =
    z.catchAllDefect { case defect: Any =>
      ZIO.succeed(
        ("Unhandled defect: " + defect).take(47)
      )
    }
  unsafeRun(res)

/** final def unsafeRun[E, A]( zio: => ZIO[R, E,
  * A] ): A = unsafeRunSync(zio) .getOrElse(c =>
  * throw FiberFailure(c))
  */
