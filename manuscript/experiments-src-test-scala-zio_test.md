## experiments-src-test-scala-zio_test

 

### experiments/src/test/scala/zio_test/Shared.scala
```scala
package zio_test

import zio.{Ref, Scope, ZIO, ZLayer}

object Shared:
  val layer: ZLayer[Any, Nothing, Ref[Int]] =
    ZLayer.scoped {
      ZIO.acquireRelease(
        Ref.make(0) <* ZIO.debug("Initializing!")
      )(
        _.get
          .debug(
            "Number of tests that used shared layer"
          )
      )
    }

  case class Scoreboard(value: Ref[Int]):
    def display(): ZIO[Any, Nothing, String] =
      for current <- value.get
      yield s"**$current**"

  val scoreBoard: ZLayer[
    Scope with Ref[Int],
    Nothing,
    Scoreboard
  ] =
    for
      value <- ZLayer.service[Ref[Int]]
      res <-
        ZLayer.scoped[Scope] {
          ZIO.acquireRelease(
            ZIO.succeed(Scoreboard(value.get)) <*
              ZIO.debug(
                "Initializing scoreboard!"
              )
          )(_ =>
            ZIO.debug("Shutting down scoreboard")
          )
        }
    yield res
end Shared

```


### experiments/src/test/scala/zio_test/UseComplexLayer.scala
```scala
package zio_test

import zio.*
import zio.test.*
import zio_test.Shared.Scoreboard

object UseComplexLayer
    extends ZIOSpec[Scoreboard]:
  def bootstrap
      : ZLayer[Any, Nothing, Scoreboard] =
    ZLayer.make[Scoreboard](
      Shared.layer,
      Shared.scoreBoard,
      Scope.default
    )

  def spec =
    test("use scoreboard") {
      for _ <-
          ZIO
            .serviceWithZIO[Scoreboard](
              _.display()
            )
            .debug
      yield assertCompletes
    }
end UseComplexLayer

```


### experiments/src/test/scala/zio_test/UseSharedLayerA.scala
```scala
package zio_test

import zio.test.{TestAspect, ZIOSpec, assertCompletes}
import zio.*

object UseSharedLayerA extends ZIOSpec[Ref[Int]]:
  def bootstrap = Shared.layer

  def spec =
    test("Test A") {
      for _ <-
          ZIO.serviceWithZIO[Ref[Int]](
            _.update(_ + 1)
          )
      yield assertCompletes
    }

```


### experiments/src/test/scala/zio_test/UseSharedLayerB.scala
```scala
package zio_test

import zio.test.{
  TestAspect,
  ZIOSpec,
  assertCompletes
}
import zio.{Ref, Scope, ZIO, ZLayer}

object UseSharedLayerB extends ZIOSpec[Ref[Int]]:
  def bootstrap = Shared.layer

  def spec =
    test("Test B") {
      for _ <-
          ZIO.serviceWithZIO[Ref[Int]](count =>
            count.update(_ + 1)
          )
      yield assertCompletes
    }

```

