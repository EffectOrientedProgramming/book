package resourcemanagement

import zio.Console.printLine

case class Slot(id: String)
case class Player(name: String, slot: Slot)

object ChatSlots extends zio.ZIOAppDefault:
  enum SlotState:
    case Closed,
      Open

  def run =

    @annotation.nowarn
    def acquire(ref: Ref[SlotState]) =
      defer:
        printLine:
          "Took a speaker slot"
        .run
        ref
          .set:
            SlotState.Open
          .run
        "Use Me"

    def release(ref: Ref[SlotState]) =
      defer:
        printLine:
          "Freed up a speaker slot"
        .orDie
          .run
        ref
          .set:
            SlotState.Closed
          .run

    defer {
      val ref =
        Ref
          .make[SlotState]:
            SlotState.Closed
          .run
      val managed =
        ZIO.acquireRelease(acquire(ref))(
          _ =>
            release:
              ref
        )
      val reusable =
        managed.map:
          printLine(_)
      reusable.run
      reusable.run
      ZIO
        .scoped:
          // TODO Get rid of flatmap if
          // possible...
          managed.flatMap:
            s =>
              defer:
                printLine:
                  s
                .run
                printLine:
                  "Blowing up"
                .run
                if (true)
                  throw Exception:
                    "Arggggg"
        .run
    }
  end run
end ChatSlots
