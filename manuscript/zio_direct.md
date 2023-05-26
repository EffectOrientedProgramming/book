---
We don't need to talk about monads, etc as zio-direct will just be "the way"
so we don't need to explain why zio-direct or what it is. It just is.
---
We have been experimenting with the zio-direct style of writing ZIO applications.
Our theory is that it is easier to teach this style of code to beginners.
"Program as values" is a core concept when using ZIO. 
ZIOs are just unexecuted values until they are run.

If not using zio-direct, you must explain many language details in order to write ZIO code.

    - If you want to sequence ZIO operations, you need to `flatmap` them
    - To avoid indentation mountain, you should use a `for comprehension`
    - How a `for` comprehension turns into `flatMap`s followed by a final `map`
    - If you want to create a `val` in the middle of this, you need to use a `=` instead of `<-`

After you have accomplished all of that, you have trained your student to write concise, clean code... that only makes sense to those versed in this style.

With zio-direct, you can write ZIO code that looks like imperative code.

Here are the concepts you need to understand for `zio-direct`

    - If you want to sequence ZIO operations, you need to write them inside of a `defer` block
    - Code in `defer` will be captured in a ZIO
    - Inside defer you can use `.run` to indicate when effects should be executed
        Note- `.run` calls are *only* allowed inside `defer` blocks.


After you have accomplished _that_, you have trained your student to write slightly less concise code... that most programmers will be comfortable with.

*Gotchas*
    -Something about mutable collection operations. TODO More info from James
    - Cannot end a defer block with a `ZIO[_,_,Nothing]`
        It currently fails with a very cryptic missing argument message


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

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

