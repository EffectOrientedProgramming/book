package resourcemanagement

import zio.Console.printLine
import zio.{Ref, ZIO}

case class Slot(id: String)
case class Player(name: String, slot: Slot)
case class Game(a: Player, b: Player)

object ChatSlots extends zio.ZIOAppDefault:
  enum SlotState:
    case Closed,
      Open

  def run =

    def acquire(ref: Ref[SlotState]) =
      for
        _ <-
          printLine {
            "Took a speaker slot"
          }
        _ <- ref.set(SlotState.Open)
      yield "Use Me"

    def release(ref: Ref[SlotState]) =
      for
        _ <-
          printLine("Freed up a speaker slot")
            .orDie
        _ <- ref.set(SlotState.Closed)
      yield ()

    for
      ref <-
        Ref.make[SlotState](SlotState.Closed)
      managed =
        ZIO.acquireRelease(acquire(ref))(_ =>
          release(ref)
        )
      reusable =
        managed.map(
          printLine(_)
        ) // note: Can't just do (Console.printLine) here
      _ <- reusable
      _ <- reusable
      _ <-
        ZIO.scoped {
          managed.flatMap { s =>
            for
              _ <- printLine(s)
              _ <- printLine("Blowing up")
              _ <- ZIO.fail("Arggggg")
            yield ()
          }
        }
    yield ()
    end for
  end run
end ChatSlots
