package zio_helpers

extension (z: ZIO.type)
  def repeatNPar[R, E, A](
      numTimes: Int
  )(op: Int => ZIO[R, E, A]): ZIO[R, E, Seq[A]] =
    z.foreachPar(0 until numTimes)(op)

  def repeatNPar[R, E, A](
      numTimes: Int
  )(op: ZIO[R, E, A]): ZIO[R, E, Seq[A]] =
    z.foreachPar(0 until numTimes)(_ => op)

extension [R, E, A](z: ZIO[R, E, A])
  def timedSecondsDebug(
      message: String
  ): ZIO[R, E, A] =
    z.timed
      .tap: (duration, res) =>
        res match
          // Don't bother printing Unit results
          case () =>
            ZIO.debug:
              message + " [took " +
                duration.getSeconds + "s]"
          case _ =>
            ZIO.debug:
              message + ": " + res + " [took " +
                duration.getSeconds + "s]"
      .map(_._2)
