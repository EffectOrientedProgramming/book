package cancellation

def createProcess(
    label: String,
    innerProcess: ZIO[Any, Nothing, Unit]
) =
  defer:
    ZIO.debug(s"Beginning $label").run
    innerProcess.run
    ZIO.debug(s"Completed $label").run
    // TODO Consider rewriting to avoid
    // dot-chaining on block
  .onInterrupt(ZIO.debug(s"Interrupt $label"))

object CancellationTree extends ZIOAppDefault:
  def spawnLevel(
      level: Int,
      limit: Int,
      parent: String
  ): ZIO[Any, Nothing, Unit] =
    ZIO
      .foreachPar(List("L", "R"))(branch =>
        val label =
          " " *
            (level * 2) + parent +
            s"-$branch"

        createProcess(
          label,
          defer:
            ZIO
              .when(level < limit):
                spawnLevel(
                  level + 1,
                  limit,
                  label
                )
              .unit
              .run
            ZIO.sleep:
              Duration.Infinity
            .run
        )
      )
      .unit

  def run =
    spawnLevel(0, 1, "Root").timeout(1.seconds)
end CancellationTree