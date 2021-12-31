package resourcemanagement

import zio.Console
import zio.{Ref, ZIO, ZRef, ZManaged}

object Trivial extends zio.ZIOAppDefault:
  enum ResourceState:
    case Closed, Open

  def run =

    def acquire(ref: Ref[ResourceState]) =
      for
        _ <-
          Console.printLine("Opening Resource")
        _ <- ref.set(ResourceState.Open)
      yield "Use Me"

    def release(ref: Ref[ResourceState]) =
      for
        _ <-
          Console
            .printLine("Closing Resource")
            .orDie
        _ <- ref.set(ResourceState.Closed)
      yield ()

    def releaseSymbolic(
        ref: Ref[ResourceState]
    ) =
      Console
        .printLine("Closing Resource")
        .orDie *> ref.set(ResourceState.Closed)

    // This combines creating a managed resource
    // with using it.
    // In normal life, users just get a managed
    // resource from
    // a library and so they don't have to think
    // about acquire
    // & release logic.
    for
      ref <-
        ZRef.make[ResourceState](
          ResourceState.Closed
        )
      managed =
        ZManaged.acquireRelease(acquire(ref))(
          release(ref)
        )
      reusable =
        managed.use(
          Console.printLine(_)
        ) // note: Can't just do (Console.printLine) here
      _ <- reusable
      _ <- reusable
      _ <-
        managed.use { s =>
          for
            _ <- Console.printLine(s)
            _ <- Console.printLine("Blowing up")
            _ <- ZIO.fail("Arggggg")
          yield ()
        }
    yield ()
    end for
  end run
end Trivial
