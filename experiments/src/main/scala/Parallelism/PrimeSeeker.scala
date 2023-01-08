package Parallelism

import zio.*

import java.math.BigInteger

object PrimeSeeker extends ZIOAppDefault:

  override def run =
    ZIO.withParallelism(1) {
      ZIO.foreachPar(1 to 16) { _ =>
        ZIO.succeed(
          crypto.nextPrimeAfter(100_000_000)
        )
      }
    } *> ZIO.debug("Found a bunch of primes")
