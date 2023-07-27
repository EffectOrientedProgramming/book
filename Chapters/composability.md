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
    - Start executing immediately
    - Must all fail with Exception
- Implicits
  - Are not automatically managed by the compiler, you must explicitly add each one to your parent function
- Try-with-resources
  - These are statically scoped
  - Unclear who is responsible for acquisition & cleanup

Each of these approaches gives you benefits, but you can't assemble them all together.
Instead of the best of all worlds, you get the pain of all worlds.
eg `Closeable[Future[Either[Throwable, A]]]`
The ordering of the nesting is significant, and not easily changed.

The number of combinations is something like:
  PairsIn(numberOfConcepts)





`Composability` has a diverse collection of definitions, depending on who you ask.

## {{Maybe goes into resources chapter}}
```scala mdoc
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

```scala mdoc:invisible
def magicalExampleCleanup() =
  availableTelescope = Some(Telescope())
```

Some possible meanings:

### "This code calls other code"
We consider this the weakest form of composability.
Your function invokes other functions, without any strict sequencing or pipelining of results.
```scala mdoc:nest
var scope: Option[Telescope] = None

def composedLogic(): Unit =
  scope = bookTelescope()
  observe(Moon, scope)

composedLogic()
```
```scala mdoc:invisible
magicalExampleCleanup()
```
In this situation, we can see that only 2 functions are being called, but this is not strict.
We could drop additional statements/effects in our block that are not visible to callers by the signature.

```scala mdoc:nest
// TODO Decide if this stage is actually helpful, or if we should just move straight into the other
// definitions and more quickly highlight the resource leak here.

var scope: Option[Telescope] = None

def destroy(): Unit =
  println("Whoops! We destroyed the telescope!")
  scope = None

def composedLogic(): Unit =
  scope = bookTelescope()
  destroy()
  observe(Moon, scope)

composedLogic()
```
```scala mdoc:invisible
magicalExampleCleanup()
```

### Mathematic definition
"You can plug the result of 1 function directly into the input of another function"
```scala mdoc
def observeDefinite(
    target: Target,
    scope: Telescope
): Unit = println(s"Looking at $target!")
```
```scala mdoc:nest
// TODO WhereTF are `compose`, `andThen`, etc?
def acquireAndObserve(target: Target): Unit =
  // Dumb way to get around an ignored unit error
  assert(
    bookTelescope()
      .map(observeDefinite(target, _)) != null
  )

acquireAndObserve(Moon)
```
```scala mdoc:invisible
magicalExampleCleanup()
```

Here, there is no ability to sneak additional statements into the code.
The functions are perfectly fused together.
This helps prevent some surprises, but still leaves us open to some problems.

```scala mdoc
// Demonstrates leaked resource

acquireAndObserve(Moon)
acquireAndObserve(Comet)
```
```scala mdoc:invisible
magicalExampleCleanup()
```
Now we see the flaw that has been lurking in our code - we haven't been relinquishing the `Telescope` after using it!
This is a classic, severe resource leak.
Our code can only use the telescope a single time before its permanently unavailable.

// TODO Demo Try-with-resources
// TODO Show how Try-with-resources does not cover our needs during dependency injection
