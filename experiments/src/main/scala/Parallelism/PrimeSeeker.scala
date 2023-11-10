package Parallelism

object PrimeSeeker extends ZIOAppDefault:

  override def run =
//    ZIO
//      .withParallelism(1) {
      ZIO.foreachPar(1 to 16) { _ =>
        ZIO
          .succeed(
            crypto.nextPrimeAfter(100_000_000)
          )
          .debug("Found prime:")
      }
//      }
      .timed
      .debug("Found a bunch of primes")
