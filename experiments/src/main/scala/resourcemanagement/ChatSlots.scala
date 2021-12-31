package resourcemanagement

import zio.Console
import zio.{Ref, ZIO, ZRef, ZManaged}

case class Slot(id: String)
case class Player(name: String, slot: Slot)
case class Game(a: Player, b: Player)

object ChatSlots extends zio.ZIOAppDefault:
  enum SlotState:
    case Closed, Open

  def run =

    def acquire(ref: Ref[SlotState]) = for
      _ <- Console.printLine("Took a speaker slot")
      _ <- ref.set(SlotState.Open)
    yield "Use Me"

    def release(ref: Ref[SlotState]) = for
      _ <- Console.printLine("Freed up a speaker slot").orDie
      _ <- ref.set(SlotState.Closed)
    yield ()

    for
      ref <- ZRef.make[SlotState](SlotState.Closed)
      managed = ZManaged.acquireRelease(acquire(ref))(release(ref))
      reusable = managed.use(Console.printLine(_)) // note: Can't just do (Console.printLine) here
      _ <- reusable
      _ <- reusable
      _ <- managed.use { s =>
        for
          _ <- Console.printLine(s)
          _ <- Console.printLine("Blowing up")
          _ <- ZIO.fail("Arggggg")
        yield ()
      }
    yield ()


