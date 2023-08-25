# Composability

Other framings/techniques and their pros/cons:
- Plain functions that throw Exceptions
  - We can't union these error possibilities and track them in the type system
- Plain functions that block
  - We can't indicate if they block or not
  - Too many concurrent blocking operations can prevent progress of other operations
  - Very difficult to manage
  - Blocking performance varies wildly between environments
- Functions that return Either/Option/Try/etc
    - We can manage the errors in the type system, but we can't interrupt the code
      that is producing these values
    - All of these types must be manually transformed into the other types
- Functions that return a Future
    - Can be interrupted example1[^^future_interrupted_1] two[^^future_interrupted_2]
    - Manual management of cancellation
    - Start executing immediately
    - Must all fail with Exception
- Implicits
  - Are not automatically managed by the compiler, you must explicitly add each one to your parent function
  - Resolving the origin of a provided implicit can be challenging
- Try-with-resources
  - These are statically scoped
  - Unclear who is responsible for acquisition & cleanup

Each of these approaches gives you benefits, but you can't assemble them all together.
Instead of the best of all worlds, you get the pain of all worlds.
eg `Closeable[Future[Either[Throwable, A]]]`
The ordering of the nesting is significant, and not easily changed.

The number of combinations is something like:
  PairsIn(numberOfConcepts)

Universal Composability with ZIO

ZIOs compose including errors, async, blocking, resource managed, cancellation, eitherness, environmental requirements.

The types expand through generic parameters. ie composing a ZIO with an error of `String` with a ZIO with an error of `Int` results in a ZIO with an error of `String | Int`.

With functions there is one way to compose. `f(g(h))` will sequentially apply the functions from the inside out.  Another term for this form of composition is called `andThen` in Scala.

With ZIO you can do an `andThen` to compose ZIOs sequentially with:
```scala
defer {
  val asdf = ZIO.succeed("asdf").run
  ZIO.succeed(asdf.toUpperCase).run
}
// res0: ZIO[Any, Nothing, String] = OnSuccess(
//   trace = "zio.direct.ZioMonad.Success.$anon.flatMap(ZioMonad.scala:19)",
//   first = Sync(
//     trace = "repl.MdocSession.MdocApp.res0(98_composability.md:8)",
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$14242/0x0000000103b81440@5e40cdad
//   ),
//   successK = repl.MdocSession$MdocApp$$Lambda$17819/0x0000000104363040@76747e01
// )
```

There are many other ways you can compose ZIOs.  The methods for composability depend on the desired behavior.  For example, to compose a ZIO that can produce an error with a ZIO that logs the error and then produces a default value, you can use the `catchAll` like:

```scala
ZIO
  .attempt("asdf")
  .catchAll { e =>
    defer {
      ZIO.logError(e.getMessage).run
      ZIO.succeed("default value").run
    }
  }
// res1: ZIO[Any, Nothing, String] = OnSuccessAndFailure(
//   trace = "repl.MdocSession.MdocApp.res1(98_composability.md:25)",
//   first = OnSuccess(
//     trace = "repl.MdocSession.MdocApp.res1(98_composability.md:19)",
//     first = Sync(
//       trace = "repl.MdocSession.MdocApp.res1(98_composability.md:19)",
//       eval = zio.ZIOCompanionVersionSpecific$$Lambda$14242/0x0000000103b81440@2cfb2c2b
//     ),
//     successK = zio.ZIO$$$Lambda$14244/0x0000000103b86840@eea09c1
//   ),
//   successK = zio.ZIO$$Lambda$14255/0x0000000103b93040@393c49d2,
//   failureK = zio.ZIO$$Lambda$14256/0x0000000103b93840@61c9c6fd
// )
```

[^^future_interrupted_1]: This is an endnote

    With multiple lines

[^^future_interrupted_2]: This is an endnote with mdoc

    ```scala
    ZIO
      .attempt("asdf")
      .catchAll { e =>
        defer {
          ZIO.logError(e.getMessage).run
          ZIO.succeed("default value").run
        }
      }
    ```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/98_composability.md)
