package resourcemanagement

object Trivial extends zio.ZIOAppDefault:
  enum ResourceState:
    case Closed,
      Open

  def run =

    def acquire(ref: Ref[ResourceState]) =
      defer {
        Console.printLine("Opening Resource").run
        ref.set(ResourceState.Open).run
        "Use Me"
      }

    def release(ref: Ref[ResourceState]) =
      defer {
        ZIO.debug("Closing Resource").run
        ref.set(ResourceState.Closed).run
      }

    // This combines creating a managed resource
    // with using it.
    // In normal life, users just get a managed
    // resource from
    // a library and so they don't have to think
    // about acquire
    // & release logic.
    defer {
      val ref =
        Ref
          .make[ResourceState](
            ResourceState.Closed
          )
          .run
      val managed =
        ZIO.acquireRelease(acquire(ref))(_ =>
          release(ref)
        )

      val reusable =
        ZIO.scoped {
          managed.map(ZIO.debug(_))
        } // note: Can't just do (Console.printLine) here
      reusable.run
      reusable.run
      ZIO
        .scoped {
          managed.flatMap { s =>
            defer {
              ZIO.debug(s).run
              ZIO.debug("Blowing up").run
              ZIO.fail("Arggggg").run
              ()
            }
          }
        }
        .run
    }
  end run
end Trivial
