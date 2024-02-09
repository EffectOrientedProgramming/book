package zio_helpers

extension [R, E, A](z: ZIO[R, E, A])
  def timedSecondsDebug(
      message: String
  ): ZIO[R, E, A] =
    z.timed
      .tap: (duration, _) =>
        println(message + " [took " +
            duration.getSeconds + "s]")
        ZIO.unit
      .map(_._2)
