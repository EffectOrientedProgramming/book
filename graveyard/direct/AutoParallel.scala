package direct

object AutoParallel extends ZIOAppDefault:

  override def run =
    def z(u1: Unit, u2: Unit) =
      println("done")

    // Multiple runs in the same expression can
    // be automatically
    // parallelized with Use.withParallelEval
    defer {
      // this will only take 5 seconds
      defer(Use.withParallelEval) {
        (
          ZIO.sleep(5.seconds).run,
          ZIO.sleep(5.seconds).run
        )
      }.timed.debug.run

      // this will only take 5 seconds
      defer(Use.withParallelEval) {
        z(
          ZIO.sleep(5.seconds).run,
          ZIO.sleep(5.seconds).run
        )
      }.timed.debug.run

      // this will not parallelize and will take
      // 10 seconds
      defer(Use.withParallelEval) {
        ZIO.sleep(5.seconds).run
        ZIO.sleep(5.seconds).run
      }.timed.debug.run
    }
  end run
end AutoParallel
