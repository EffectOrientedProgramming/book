package mutability

import zio.{Ref, ZIO, ZIOAppDefault}

import java.lang

object UnsafeEffectsInsideAtomicRefs extends ZIOAppDefault {

  var updateAttempts = 0
  val reliableCounting =
    for
      counter <- Ref.make(0)
      _ <-
        ZIO.foreachParDiscard(Range(0, 10000))(i =>
          counter.update{previousValue =>
            // This is dangerous because using a non-synchronized Ref might retry this block many times before succeeding
            // Pure functions can be re-executed an arbitrary number of times, but side effects have to happen exactly once.
            updateAttempts += 1
            previousValue + 1
          }
        )
      finalResult <- counter.get
    yield "Final count: " + finalResult + "  updateAttempts: " + updateAttempts

  def run = reliableCounting.debug

}
