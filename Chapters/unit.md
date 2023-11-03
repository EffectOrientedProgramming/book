# Unit

Note - This is a little long to be absorbed into the Composability chapter...

### The bare minimum of effect tracking

Consider a function

```scala mdoc
def saveInformation(info: String): Unit = ???
```

If we consider only the types, this function is an `Any=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we see it, we know that *some* type of side-effect is being performed, but without any specificity.

When a function returns `Unit`, we know that the only reason we are calling the function is to perform an effect.
Alternatively, if there are no arguments to the function, then the input is `Unit`, indicating that an effect is used to _produce_ the result.

Consider a `WeatherService` API:

```scala mdoc
trait WeatherService:
  def forecast(): String
```

If we do not have access to the implementation source code, there is no way to discern what effects are needed at compile time.
The only way to figure it out is to run the code and see what happens.

```scala mdoc:invisible
class ClosedSourceWeatherService
    extends WeatherService:
  def forecast(): String =
    println("READ GPS SIGNAL")
    println("NETWORK CALL")
    "Sunny"
```

```scala mdoc
ClosedSourceWeatherService().forecast()
```

It is possible that we are using entirely open-source or in-house code throughout our entire application.
That means that we could theoretically dig into every function involved in a complex path and note every effect.

In practice this quickly becomes impossible.
Note - We indicate the external system that we are interacting with by printing the all-caps value.

```scala mdoc
object Analytics:
  def demographicsFrom(userData: String): Unit =
    println("LOGGER: Key demographic found")

object OpenSourceLibrary:
  private def save(userData: String): Unit =
    Analytics.demographicsFrom(userData)
    println("DATABASE: Saving data")

  def sendToService(payload: String): Unit =
    println("NETWORK: Sending payload")
    save(payload)
```


```scala mdoc
def logic(): Unit =
  // ...Other calls...
  OpenSourceLibrary
    .sendToService("Network Payload")
  // ...Other calls...

logic()
```

Here our program performs 3 very different side-effects, but everything is boiled down to the same `Unit` type.
If we extrapolate this to a production application with hundreds and thousands of functions, it becomes overwhelming.

Ideally, we could leverage the type system and the compiler to track the requirements for arbitrarily complex pieces of code.