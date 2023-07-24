package layers

trait Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit]

object ConsoleLive extends Console:
  def printLine(
      output: String
  ): ZIO[Any, Nothing, Unit] =
    // TODO Get this working without Predef
    ZIO.succeed(Predef.println(output))

case class Logic(console: Console):
  val invoke: ZIO[Any, Nothing, Unit] =
    defer {
      console.printLine("Hello").run
      console.printLine("World").run
    }

object Console:
  val live: ZLayer[Any, Nothing, Console] =
    ZLayer.succeed[Console](ConsoleLive)

object Creation extends ZIOAppDefault:
  def run =
    ZIO
      .serviceWithZIO[Logic](_.invoke)
      .provide(
        Console.live,
        ZLayer.fromFunction(Logic.apply _)
      )
