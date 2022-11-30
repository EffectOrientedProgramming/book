# Composability

`Composability` has a diverse collection of definitions, depending on who you ask.

```scala mdoc
enum Target:
    case Moon, Planet, Comet, Star, Galaxy
    
import Target._
case class Telescope()

var availableTelescope: Option[Telescope] = Some(Telescope())

def observe(target: Target, scope: Option[Telescope]): Unit = 
    scope match
        case Some(telescope) =>
            println(s"Looking at $target!")
        case None =>
            println(s"Telescope unavailable! Cannot view $target!")
            
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
def observeDefinite(target: Target, scope: Telescope): Unit = 
    println(s"Looking at $target!")

```
```scala mdoc:nest

// TODO WhereTF are `compose`, `andThen`, etc?
def acquireAndObserve(target: Target): Unit =
    bookTelescope().map(observeDefinite(target, _))
    
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