---
---

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
    - Can be interrupted
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
//     trace = "repl.MdocSession.MdocApp.res0(composability.md:8)",
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$2086/0x0000000100a7fc40@77e5ff6a
//   ),
//   successK = repl.MdocSession$MdocApp$$Lambda$4006/0x0000000100fa9840@2fc5c738
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
//   trace = "repl.MdocSession.MdocApp.res1(composability.md:25)",
//   first = OnSuccess(
//     trace = "repl.MdocSession.MdocApp.res1(composability.md:19)",
//     first = Sync(
//       trace = "repl.MdocSession.MdocApp.res1(composability.md:19)",
//       eval = zio.ZIOCompanionVersionSpecific$$Lambda$2086/0x0000000100a7fc40@2af2e677
//     ),
//     successK = zio.ZIO$$$Lambda$2088/0x0000000100a7d840@4a119605
//   ),
//   successK = zio.ZIO$$Lambda$2099/0x0000000100a93840@2c5b2fcd,
//   failureK = zio.ZIO$$Lambda$2100/0x0000000100a94040@741d8b83
// )
```


`Composability` has a diverse collection of definitions, depending on who you ask.

## {{Maybe goes into resources chapter}}
```scala
enum Target:
  case Moon,
    Planet,
    Comet,
    Star,
    Galaxy

import Target._
case class Telescope()

var availableTelescope: Option[Telescope] =
  Some(Telescope())
// availableTelescope: Option[Telescope] = Some(value = Telescope())

def observe(
    target: Target,
    scope: Option[Telescope]
): Unit =
  scope match
    case Some(telescope) =>
      println(s"Looking at $target!")
    case None =>
      println(
        s"Telescope unavailable! Cannot view $target!"
      )

def bookTelescope(): Option[Telescope] =
  availableTelescope match
    case Some(_) =>
      println("Acquired telescope!")
    case None =>
      println("Failed to acquire telescope!")
  val result = availableTelescope
  availableTelescope = None
  result
```


Some possible meanings:

### "This code calls other code"
We consider this the weakest form of composability.
Your function invokes other functions, without any strict sequencing or pipelining of results.
```scala
var scope: Option[Telescope] = None
// scope: Option[Telescope] = None

def composedLogic(): Unit =
  scope = bookTelescope()
  observe(Moon, scope)

composedLogic()
// Acquired telescope!
// Looking at Moon!
```
In this situation, we can see that only 2 functions are being called, but this is not strict.
We could drop additional statements/effects in our block that are not visible to callers by the signature.

```scala
// TODO Decide if this stage is actually helpful, or if we should just move straight into the other
// definitions and more quickly highlight the resource leak here.

var scope: Option[Telescope] = None
// scope: Option[Telescope] = None

def destroy(): Unit =
  println("Whoops! We destroyed the telescope!")
  scope = None

def composedLogic(): Unit =
  scope = bookTelescope()
  destroy()
  observe(Moon, scope)

composedLogic()
// Acquired telescope!
// Whoops! We destroyed the telescope!
// Telescope unavailable! Cannot view Moon!
```

### Mathematic definition
"You can plug the result of 1 function directly into the input of another function"
```scala
def observeDefinite(
    target: Target,
    scope: Telescope
): Unit = println(s"Looking at $target!")
```
```scala
// TODO WhereTF are `compose`, `andThen`, etc?
def acquireAndObserve(target: Target): Unit =
  // Dumb way to get around an ignored unit error
  assert(
    bookTelescope()
      .map(observeDefinite(target, _)) != null
  )

acquireAndObserve(Moon)
// Acquired telescope!
// Looking at Moon!
```

Here, there is no ability to sneak additional statements into the code.
The functions are perfectly fused together.
This helps prevent some surprises, but still leaves us open to some problems.

```scala
// Demonstrates leaked resource

acquireAndObserve(Moon)
// Acquired telescope!
// Looking at Moon!
acquireAndObserve(Comet)
// Failed to acquire telescope!
```
Now we see the flaw that has been lurking in our code - we haven't been relinquishing the `Telescope` after using it!
This is a classic, severe resource leak.
Our code can only use the telescope a single time before its permanently unavailable.

// TODO Demo Try-with-resources
// TODO Show how Try-with-resources does not cover our needs during dependency injection
