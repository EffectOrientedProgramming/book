package concurrency

import zio.{ZIO, ZIOAppDefault}

import java.math.BigInteger

object WhyZio extends ZIOAppDefault:

  override def run =
    val genPrime =
      ZIO
        .attempt {
          crypto.nextPrimeAfter(100_000_000)
        }
        .timed

    ZIO.raceAll(genPrime, Seq(genPrime)).debug
