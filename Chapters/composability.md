# Composability

`Composability` has a diverse collection of definitions, depending on who you ask.

```scala mdoc
enum Target:
    case Moon, Planet, Comet, Star, Galaxy
    
import Target._
case class Telescope()
```

Some possible meanings:

### "This code calls other code"
We consider this the weakest form of composability.
Your function invokes other functions, without any strict sequencing or pipelining of results.
```scala mdoc:nest
var scope: Option[Telescope] = None

def observe(target: Target): Unit = 
    scope match
        case Some(telescope) =>
            println(s"Looking at $target!")
        case None =>
            println(s"Telescope unavailable! Cannot view $target!")
            
def bookAvailableTelescope(): Unit = 
    println("Acquiring Telescope.")
    scope = Some(Telescope())

def composedLogic(): Unit =
    bookAvailableTelescope()
    observe(Moon)
    
composedLogic()
```
In this situation, we can see that only 2 functions are being called, but this is not strict.
We could drop additional statements/effects in our block that are not visible to callers by the signature.

```scala mdoc:nest
var scope: Option[Telescope] = None

def observe(target: Target): Unit = 
    scope match
        case Some(telescope) =>
            println(s"Looking at $target!")
        case None =>
            println(s"Telescope unavailable! Cannot view $target!")
            
def bookAvailableTelescope(): Unit = 
    println("Acquiring Telescope.")
    scope = Some(Telescope())
    
def destroy(): Unit = 
    println("Whoops! We destroyed the telescope!")
    scope = None

def composedLogic(): Unit =
    bookAvailableTelescope()
    println("Sneaky effect!")
    destroy()
    observe(Moon)

composedLogic()
```

### Mathematic definition
"You can plug the result of 1 function directly into the input of another function"
```scala mdoc:nest

def observe(scope: Telescope, target: Target): Unit = ???
def bookAvailableTelescope(): Option[Telescope] = ???

// TODO WhereTF are `compose`, `andThen`, etc?
def composedLogic(): Unit =
    bookAvailableTelescope().map(observe(_, Moon))
```
Here, there is no ability to sneak additional statements into the code.
The functions are perfectly fused together.