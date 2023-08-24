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
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$14309/0x0000000103bb1c40@d680e51
//   ),
//   successK = repl.MdocSession$MdocApp$$Lambda$17899/0x000000010468f040@5097c156
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
//       eval = zio.ZIOCompanionVersionSpecific$$Lambda$14309/0x0000000103bb1c40@1e2cc4c1
//     ),
//     successK = zio.ZIO$$$Lambda$14311/0x0000000103bb6840@74021f70
//   ),
//   successK = zio.ZIO$$Lambda$14322/0x0000000103bc3840@5f70456a,
//   failureK = zio.ZIO$$Lambda$14323/0x0000000103bc4040@1227d253
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
