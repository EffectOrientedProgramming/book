
```scala mdoc:spec
import zio.test.*

runSpec(
  assertTrue(1 == 2)
)
```

```scala mdoc
runSpec(
  assertTrue(1 == 2)
)
```

```scala mdoc:run
runDemo(
  Console.printLine("hello!")
)
```

```scala mdoc
runDemo(
  Console.printLine("hello!")
)
```


```scala mdoc
runDemo(
  ZIO.succeed(scala.Console.println("Failure!!!"))
)
```
          

```scala mdoc
runDemo(
  ZIO.succeed(println("hi"))
)
```

Passing test
```scala mdoc
import zio.test.assertTrue
runSpec(
  defer:
    Console.printLine("Spec stuff!").orDie.run
    assertTrue(1 == 1)
)
```
Failing test
```scala mdoc
runSpec(
  defer:
    Console.printLine("Spec stuff!").orDie.run
    assertTrue(1 == 2)
)
```
