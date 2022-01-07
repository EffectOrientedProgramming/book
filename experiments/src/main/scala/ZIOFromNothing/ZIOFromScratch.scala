package ZIOFromNothing

case class IO(behavior: () => Unit):
    def compose(other: IO) =
        IO(
            () => 
                behavior()
                println("New behavior from compose")
                other.behavior()
        )

object Interpreter:
    def run(io: IO) = io.behavior()


@main
def runEffects =
    val hi = IO(() => println("hi "))
    val there = IO(() => println("there!"))

    val fullApp = hi.compose(there)

    Interpreter.run(fullApp)

