package testcontainers

import zio.*
import org.testcontainers.containers.{
  GenericContainer
}

object GenericInteractionsZ:
  def manage[T <: GenericContainer[T]](
      c: T,
      containerType: String
  ) =
    ZManaged.acquireReleaseWith(
      ZIO.debug(s"Creating $containerType") *>
        start(c, containerType) *> ZIO.succeed(c)
    )((n: T) =>
      ZIO.attempt(n.close()).orDie *>
        ZIO.debug(s"Closing $containerType")
    )

  def manageWithInitialization[
      T <: GenericContainer[T]
  ](
      c: T,
      containerType: String,
      initialize: T => ZIO[
        Any,
        Throwable,
        Unit
      ] = (_: T) => ZIO.unit
  ) =
    ZManaged.acquireReleaseWith(
      ZIO.debug(s"Creating $containerType") *>
        start(c, containerType) *>
        initialize(c) *> ZIO.succeed(c)
    )((n: T) =>
      ZIO.attempt(n.close()).orDie *>
        ZIO.debug(s"Closing $containerType")
    )

  private def start[T <: GenericContainer[T]](
      c: T,
      containerType: String
  ) =
    ZIO.blocking(ZIO.succeed(c.start)) *>
      ZIO.debug(
        s"Finished blocking for $containerType container creation"
      )

end GenericInteractionsZ
