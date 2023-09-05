package cancellation

val longRunning =
  createProcess("LongRunning", ZIO.sleep(5.seconds))

def createProcess(label: String, innerProcess: ZIO[Any, Nothing, Unit]) =
  defer:
    ZIO.debug(s"Started $label").run
    innerProcess.run
    ZIO.debug(s"Finished $label").run
  .onInterrupt(
    ZIO.debug(s"Interrupted $label")
  )

object HelloCancellation extends ZIOAppDefault:

  def run = longRunning.timeout(2.seconds)

object HelloCancellation2 extends ZIOAppDefault:
  val complex =
    createProcess("Complex", longRunning)

  def run = complex.timeout(2.seconds)

object CancellationWeb extends ZIOAppDefault:
  def spawnLevel(level: Int, limit: Int, parent: String): ZIO[Any, Nothing, Unit] =
      ZIO
        .foreachPar(List("L", "R"))(label =>
          createProcess(
            " " * (level + 1 * 2) + parent + s"-$label",
            ZIO.when(level < limit)(
              spawnLevel(level + 1, limit, " " * (level + 1 * 2) + parent + s"-$label")
            ).unit
          )
      ).delay(level.seconds).unit

  def run =
    spawnLevel(0, 3, "Root")
      .timeout(3.seconds)
end CancellationWeb

object FailureDuringFork extends ZIOAppDefault:
  def run =
    defer {
      val fiber1 =
        defer:
          ZIO.sleep(5.seconds).run
          ZIO.debug("Complete fiber 1").run
        .onInterrupt(
          ZIO.debug("Interrupted fiber 1")
        ).fork
          .run

      val fiber2 =
        defer:
          ZIO.sleep(5.seconds).run
          ZIO.debug("Complete fiber 2").run
        .onInterrupt(
          ZIO.debug("Interrupted fiber 2")
        ).fork
          .run

      // Once we fail here, the fibers will be
      // interrupted.
      ZIO.fail("Youch!").run
      fiber1.join.run
      fiber2.join.run
    }
end FailureDuringFork
