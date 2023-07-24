package Parallelism

import java.lang.Runtime as JavaRuntime

/* This example was a port of Bruce's Python
 * example:
 * https://github.com/BruceEckel/python-experiments/blob/main/parallelism/cpu_intensive.py
 *
 * We did this port to explore some very basic
 * parallelism.
 * We learned how in ZIO we generally shouldn't
 * base concurrency on the number of system cores
 * because ZIO takes care of that for us. */
object UseAllTheThreads extends ZIOAppDefault:

  def cpuIntensive(
      n: Int,
      multiplier: Int
  ): Double =
    /* // mutable is fast var result: Double = 0
     * for (i <- 0 until 10_000_000 * multiplier)
     * { result += Math.sqrt(Math.pow(i, 3) +
     * Math.pow(i, 2) + i * n) } result */
    /* // this is very slow due to massive
     * allocations (0 until 10_000_000 *
     * multiplier).map { i =>
     * Math.sqrt(Math.pow(i, 3) + Math.pow(i, 2)
     * + i * n) }.sum */
    // pretty fast and immutable
    (0 until 10_000_000 * multiplier)
      .foldLeft(0d) { (result, i) =>
        result +
          Math.sqrt(
            Math.pow(i, 3) + Math.pow(i, 2d) +
              i * n
          )
      }

  override def run =
    defer {
      // note that we used numCores to show how
      // we can saturate all of them
      // but it turns out that even with a number
      // less than the number of cores
      // we can still saturate all of them
      // because the ZIO scheduler time slices
      val numCores =
        JavaRuntime
          .getRuntime
          .availableProcessors
      ZIO
        .foreachPar(0 to numCores) { i =>
          ZIO.succeed(cpuIntensive(i, 10))
        }
        .debug
        .run
    }
end UseAllTheThreads
