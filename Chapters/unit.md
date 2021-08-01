# Unit

### The bare minimum of effect tracking 
TODO proper subheading approach. This is not a real "section"

Consider a simple function

```scala mdoc
def saveInformation(info: Any): Unit = ???
```

If we consider only the types, this function is an `Any=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we see it, we know that *some* type of side-effect is being performed, but without any specificity.

When a function returns `Unit`, we know that the result *is* an effect.
Alternatively, if there are no arguments to the function, then the input is `Unit`, indicating that an effect is used to _produce_ the result.

## Closed Source Scenario

Consider a simple `WeatherService` API:

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
    println("CHECK CLOCK")
    println("NETWORK CALL")
    println("READ GPS SIGNAL")
    "Sunny"
end ClosedSourceWeatherService
```

```scala mdoc
ClosedSourceWeatherService().forecast()
```

What initially seemed like a simple call now reveals that it had 3 distinct effect dependencies!
The first call to the system `Clock` will probably never fail, but does make this function difficult to test.
The next 2 calls are much more likely to cause problems.
If the machine executing the code does not have a network connection, or has an unreliable one, this code will fail.
Further, if the machine does not have GPS hardware, this code will _never_ succeed.

## Open Source Scenario


```scala mdoc
trait LibraryCode:
  def crunchNumbersAndPrintResult(
      x: Int,
      y: Int
  ): Unit

  def longRunningNetworkSubmission(
      payload: String
  ): Unit

  def saveUserInfoToDatabase(
      userData: String
  ): Unit
end LibraryCode
```

TODO Make this the open-source version instead
```scala mdoc:invisible
class ClosedSourceCodeImpl() extends LibraryCode:
  def crunchNumbersAndPrintResult(
      x: Int,
      y: Int
  ): Unit =
    println(
      s"CONSOLE: Result of crunching $x and $y"
    )

  def longRunningNetworkSubmission(
      payload: String
  ): Unit = println(s"NETWORK: Sending payload")

  def saveUserInfoToDatabase(
      userData: String
  ): Unit = println(s"DATABASE: Saving data")
end ClosedSourceCodeImpl
```


```scala mdoc
def logic(): Unit =
  val library: LibraryCode =
    ClosedSourceCodeImpl()
  library.crunchNumbersAndPrintResult(3, 5)
  library.longRunningNetworkSubmission(
    "Network Payload"
  )
  library.saveUserInfoToDatabase("User Info")

logic()
```

Here our simple program performs 3 very different side-effects, but everything is boiled down to the same `Unit` type.
If we extrapolate this is to a production application with hundreds and thousands of functions, it is overwhelming.

It is possible that we are using entirely open-source or in-house code throughout our entire application.
That means that we could theoretically dig into every function involved in a complex path and note every effect.

In practice this quickly becomes impossible.

Ideally, we could leverage the type system and the compiler to track the requirements for arbitrarily complex pieces of code.