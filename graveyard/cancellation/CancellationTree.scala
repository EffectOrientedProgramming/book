package cancellation

def createProcess(
    label: String,
    innerProcess: ZIO[Any, Nothing, Unit]
) =
  defer:
    ZIO.debug(s"Beginning $label").run
    innerProcess.run
    ZIO
      .sleep:
        Duration.Infinity
      .run
  .onInterrupt(ZIO.debug(s"Interrupt $label"))

object CancellationTree extends ZIOAppDefault:
  def spawnChildren(
      level: Int,
      limit: Int,
      parent: String
  ): ZIO[Any, Nothing, Unit] =
    ZIO
      .foreachPar(List("L", "R"))(
        branch =>
          val label =
            " " *
              (level * 2) + parent + s"-$branch"

          createProcess(
            label,
            ZIO
              .when(level < limit):
                spawnChildren(
                  level + 1,
                  limit,
                  label
                )
              .unit
          )
      )
      .unit

  def run =
    spawnChildren(0, 1, "Root")
      .timeout(1.seconds)
end CancellationTree
