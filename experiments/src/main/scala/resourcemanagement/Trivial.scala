package resourcemanagement

import zio.Console
import zio.{ZIO, ZRef, ZManaged}

object Trivial extends zio.ZIOAppDefault:
  enum ResourceState:
    case Closed, Open

  def run =
    for
      ref <- ZRef.make[ResourceState](ResourceState.Closed)
      managed = ZManaged.acquireRelease(
        for
          _ <- Console.printLine("Opening Resource")
          _ <- ref.set(ResourceState.Open)
        yield "Use Me"
      )(
        for
          _ <- Console.printLine("Closing Resource").orDie
          _ <- ref.set(ResourceState.Closed)
        yield ()
      )
      reusable = managed.use(Console.printLine(_)) // note: Can't just do (Console.printLine) here
      _ <- reusable
      _ <- reusable
    yield ()


