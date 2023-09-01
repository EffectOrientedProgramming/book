```scala
import zio.test._
object HelloSpec extends ZIOSpecDefault:
  def spec =
    test("HelloSpec")(assertCompletes)
```

```scala
runSpec(
  assertTrue(2 == 4)
)
// 
// [31m- [31m[0m[0m
//   [31m✗ [0m[1m[34m2[0m[0m [31mwas not e
//   [1m2 == [0m[1m[33m4[0m[0m[1m[0m
//   [36mat zio_test.md:19 [0m
```

```scala
runSpec(
  defer {
    println("hi")
    assertTrue(2 == 4)
  }
)
// hi
// 
// [31m- [31m[0m[0m
//   [31m✗ Result was false[0m
//   [1m[0m[1m[33m2 == 4[0m[0m[1m[0m
//   [36mat zio_test.md:28 [0m
```

```scala
object HelloSpec2 extends ZIOSpecDefault:
    def spec =
      test("HelloSpec")(
        defer:
          ZIO.debug("hi").run
          val res = ZIO.succeed(42).run
          assertTrue(
            res == 43
          )
      )
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/zio_test.md)
