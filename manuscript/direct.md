## direct

 

### experiments/src/main/scala/direct/AutoParallel.scala
```scala
package direct

import zio.*
import zio.direct.*

object AutoParallel extends ZIOAppDefault:

  override def run =
    def z(u1: Unit, u2: Unit) = println("done")

    // Multiple runs in the same expression can
    // be automatically
    // parallelized with Use.withParallelEval
    defer {
      // this will only take 5 seconds
      defer(Use.withParallelEval) {
        (
          ZIO.sleep(5.seconds).run,
          ZIO.sleep(5.seconds).run
        )
      }.timed.debug.run

      // this will only take 5 seconds
      defer(Use.withParallelEval) {
        z(
          ZIO.sleep(5.seconds).run,
          ZIO.sleep(5.seconds).run
        )
      }.timed.debug.run

      // this will not parallelize and will take
      // 10 seconds
      defer(Use.withParallelEval) {
        ZIO.sleep(5.seconds).run
        ZIO.sleep(5.seconds).run
      }.timed.debug.run
    }
  end run
end AutoParallel

```


### experiments/src/main/scala/direct/EasyMode.scala
```scala
package direct

import zio.*
import zio.direct.*

import java.io.IOException

// "run this effect" style
// Note: in ZIOAppDefault "run" conflicts
val runHello =
  defer {
    run(Console.printLine("hello"))
  }

object EasyMode extends ZIOAppDefault:

  // note: this needs to be in a defer block
  //       but it is not and results in a runtime error
  // val r = Console.printLine("hello,
  // world").run

  val d =
    defer {
      // Note: It is a compile error to have an
      // unused effect
      //       in defer which is not run
      // Console.printLine("hello, world")

      val c = Console.printLine("hello")
      val i = Random.nextInt.run
      Console.printLine(s"i = $i").run
      c.run
    }

  val f =
    for
      _ <- Console.printLine("hello, world")
      i <- Random.nextInt
      _ <- Console.printLine(s"i = $i")
    yield ()

  val e =
    defer {
      Console.printLine("hello").run
      val r = Random.nextInt.run
      val a: ZIO[Any, IOException, IO[
        IOException,
        Unit
      ]] =
        defer { // .info prints compile-time unraveling
          Console.printLine("to").run
          Console.printLine("world").run
          Console.printLine(
            s"r = $r"
          ) // forgot the .run so this effect is the result of the outer ZIO
        }
      Console.printLine("before").run
      val z = a.run
      z.run
    }

  def get(): ZIO[Any, Nothing, Int] =
    ZIO.succeed(200)

  // throw can put something in the error channel
  val x: ZIO[Any, Exception, String] =
    defer {
      val response = get().run
      if response == 200 then
        "asdf"
      else
        throw Exception(
          "zxcv"
        ) // or ZIO.fail(Exception("zxcv")).run
    }

  override def run = d *> f *> e *> x.debug
end EasyMode

```


