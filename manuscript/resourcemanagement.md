## resourcemanagement

 

### experiments/src/main/scala/resourcemanagement/ChatSlots.scala
```scala
package resourcemanagement

import zio.Console.printLine
import zio.{Ref, ZIO}
import zio.direct.*

case class Slot(id: String)
case class Player(name: String, slot: Slot)
case class Game(a: Player, b: Player)

object ChatSlots extends zio.ZIOAppDefault:
  enum SlotState:
    case Closed,
      Open

  def run =

    def acquire(ref: Ref[SlotState]) =
      defer {
        printLine {
          "Took a speaker slot"
        }.run
        ref.set(SlotState.Open).run
        "Use Me"
      }

    def release(ref: Ref[SlotState]) =
      defer {
        printLine("Freed up a speaker slot")
          .orDie
          .run
        ref.set(SlotState.Closed).run
      }

    defer {
      val ref =
        Ref.make[SlotState](SlotState.Closed).run
      val managed =
        ZIO.acquireRelease(acquire(ref))(_ =>
          release(ref)
        )
      val reusable =
        managed.map(
          printLine(_)
        ) // note: Can't just do (Console.printLine) here
      reusable.run
      reusable.run
      ZIO
        .scoped {
          managed.flatMap { s =>
            defer {
              printLine(s).run
              printLine("Blowing up").run
              if (true)
                throw new Exception("Arggggg")
            }
          }
        }
        .run
    }
  end run
end ChatSlots

```


### experiments/src/main/scala/resourcemanagement/Trivial.scala
```scala
package resourcemanagement

import zio.Console
import zio.{Ref, ZIO}

object Trivial extends zio.ZIOAppDefault:
  enum ResourceState:
    case Closed,
      Open

  def run =

    def acquire(ref: Ref[ResourceState]) =
      for
        _ <-
          Console.printLine("Opening Resource")
        _ <- ref.set(ResourceState.Open)
      yield "Use Me"

    def release(ref: Ref[ResourceState]) =
      for
        _ <- ZIO.debug("Closing Resource")
        _ <- ref.set(ResourceState.Closed)
      yield ()

    def releaseSymbolic(
        ref: Ref[ResourceState]
    ) =
      ZIO.debug("Closing Resource") *>
        ref.set(ResourceState.Closed)

    // This combines creating a managed resource
    // with using it.
    // In normal life, users just get a managed
    // resource from
    // a library and so they don't have to think
    // about acquire
    // & release logic.
    for
      ref <-
        Ref.make[ResourceState](
          ResourceState.Closed
        )
      managed =
        ZIO.acquireRelease(acquire(ref))(_ =>
          release(ref)
        )

      reusable =
        ZIO.scoped {
          managed.map(ZIO.debug(_))
        } // note: Can't just do (Console.printLine) here
      _ <- reusable
      _ <- reusable
      _ <-
        ZIO.scoped {
          managed.flatMap { s =>
            for
              _ <- ZIO.debug(s)
              _ <- ZIO.debug("Blowing up")
              _ <- ZIO.fail("Arggggg")
            yield ()
          }
        }
    yield ()
    end for
  end run
end Trivial

```


