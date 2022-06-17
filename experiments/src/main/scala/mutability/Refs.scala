package mutability

import mutability.UnreliableMutability.incrementCounter
import zio.{Ref, ZIO, ZIOAppDefault}

object UnreliableMutability
    extends ZIOAppDefault:
  var counter = 0
  def incrementCounter() =
    ZIO.succeed {
      counter = counter + 1
      counter
    }

  def run =
    (for
      results <-
        ZIO.foreachParDiscard(Range(0, 10000))(
          _ => incrementCounter()
        ).timed
      _ <- ZIO.debug("Final count: " + counter)
      _ <- ZIO.debug("Duration: " + results._1.toMillis)
    yield ())


object ReliableMutability
  extends ZIOAppDefault:
  def incrementCounter(counter: Ref[Int]) =
    counter.update(_ + 1)

  def run =
    for
      counter <- Ref.make(0)
      results <-
        ZIO.foreachParDiscard(Range(0, 10000))(
          _ => incrementCounter(counter)
        ).timed
      finalResult <- counter.get
      _ <- ZIO.debug("Final count: " + finalResult)
      _ <- ZIO.debug("Duration: " + results._1.toMillis)
    yield ()






object Refs extends ZIOAppDefault:
  def run =
    for
      ref        <- Ref.make(1)
      startValue <- ref.get
      _ <-
        ZIO.debug("start value: " + startValue)
      _          <- ref.set(5)
      finalValue <- ref.get
      _ <-
        ZIO.debug("final value: " + finalValue)
    yield ()
