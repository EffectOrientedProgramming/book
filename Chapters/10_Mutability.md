# Mutability

```scala mdoc
import zio.{Ref, UIO, ZIO, ZIOAppDefault}
import mdoc.unsafeRunPrettyPrint
import mdoc.unsafeRunTruncate
import mdoc.wrapUnsafeZIO
import zio.Runtime.default.unsafeRun

object UnreliableMutability:
  var counter = 0
  def increment() =
    ZIO.succeed {
      counter = counter + 1
    }

  val demo: UIO[String] =
    for _ <-
        ZIO.foreachParDiscard(Range(0, 10000))(
          _ => increment()
        )
    yield "Final count: " + counter
    
  val simplerDemo = ZIO.succeed("label")

unsafeRunPrettyPrint(ZIO.succeed(1))
unsafeRunPrettyPrint(UnreliableMutability.demo)
unsafeRun(UnreliableMutability.demo)
unsafeRunPrettyPrint(UnreliableMutability.simplerDemo)
println("ah")
```