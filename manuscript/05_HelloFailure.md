# Hello Failures

```scala
// Value.scala
import prep.putStrLn

val basic = putStrLn("hello, world")
```


```scala
// Program.scala
// todo: should fail?
import prep.putStrLn
import zio.Runtime.default

val basic = putStrLn("hello, world")

@main def run = default.unsafeRunSync(basic)
```

```scala
// ZApp.scala
import prep.putStrLn
import zio.{App, ZIO}

val basic = putStrLn("hello, world")

object ZApp extends App:
  override def run(args: List[String]) = basic.catchAll(_ => ZIO.unit).exitCode
```