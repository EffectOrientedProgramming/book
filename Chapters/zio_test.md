```scala mdoc
import zio.test._
object HelloSpec extends ZIOSpecDefault:
  def spec = test("HelloSpec")(assertCompletes)
```

```scala mdoc
runSpec(assertTrue(2 == 4))
```

```scala mdoc
runSpec(
  defer {
    println("hi")
    assertTrue(2 == 4)
  }
)
```

```scala mdoc
object HelloSpec2 extends ZIOSpecDefault:
  def spec =
    test("HelloSpec")(
      defer:
        ZIO.debug("hi").run
        val res = ZIO.succeed(42).run
        assertTrue(res == 43)
    )
```
