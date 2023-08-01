package interpreter

object NeverFinalizer extends ZIOAppDefault:
  @annotation.nowarn
  def willBeCancelled
      : ZIO[Scope, Nothing, Unit] =
    defer {
      ZIO
        .debug("Starting never-ending effect")
        .run
      ZIO
        .addFinalizer(ZIO.debug("Finalizing"))
        .run
      ZIO.never.run
      ()
    }

  def run = willBeCancelled.timeout(1.second)
