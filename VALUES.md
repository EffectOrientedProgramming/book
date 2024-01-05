Reliability-Oriented Development
--------------------------------

ROD!!

### Values

1. No forward references
1. Explain the "why"
1. Code examples that work
1. If the reader likely doesn't understand a concept, it must be explained.
1. Limited feature use (no GADTs) - We rely on other more exhaustive resources for deeper topics (macros)
1. Honest - Software fails. Expect the unexpected.
1. Address what the reader will face in the real world.
1. Don't make the reader's eyes glaze over. (Not too abstract)
1. Training / Teaching oriented (Atomic - classroom length) 15 minute lecture. Then exercises.
1. Maybe videos per chapter walking through code examples
1. 20% free (chapters & videos)
1. Possible avenue for training & consulting
1. Audience is existing developers but not necessarily Scala developers
1. Scala developers can easily skip over "context" chapters
1. Test Oriented Programming (not TDD)
1. Scala 3 + ZIO 2
1. Aspirational Programming (???)
1. Braceless syntax, as much as possible

1. No for comprehensions
1. No flatmap references
1. No map references

- Don't mention monads

- runDemo & runSpec to keep examples tight
    - Rename runTest


How much do we get into underlying Monadic / Algebraic Laws?
1. "Math tells us..." (No Monad arrow / CT diagram)
1. 

How do we have a feedback loop as we build?  Can we run training while writing the book?

Given the most basic effect:

```scala
val app: ZIO[Any, Nothing, Unit] = ZIO.unit
```

We need to explain:
 - imports
 - val
 - effect
 - ZIO types `[R, E, A]`
 - unit


```scala
val a: ZIO[Any, Nothing, Int] = ZIO.pure(1)
```

Function Composition (transform value to value):

```scala
val s: ZIO[Any, Nothing, String] = ZIO.pure(1).map { i =>
  s"asdf: $i"
}
```

Function Composition (transform value to effect):

```scala
val s: ZIO[Any, Nothing, String] = ZIO.pure(1).flatMap { i =>
  ZIO.pure(s"asdf: $i")
}
```




We need to explain:
- Type inference

Given a useful effect

```scala
val r = ZIO.random()
```





Given the most basic program:


```
object Foo extends zio.App

  def main = ZIO.unsafeRun(ZIO.unit)
```

We need to have a chapter that explains:
 - object
 - Inheritance
 - def
 - ZIO
 - unit
