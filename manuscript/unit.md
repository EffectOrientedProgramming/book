# Unit

### The bare minimum of effect tracking

Consider a simple function

```scala
def saveInformation(info: Any): Unit = ???
```

If we consider only the types, this function is an `Any=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we see it, we know that *some* type of side-effect is being performed, but without any specificity.

When a function returns `Unit`, we know that the result *is* an effect.
Alternatively, if there are no arguments to the function, then the input is `Unit`, indicating that an effect is used to _produce_ the result.

Consider a simple `WeatherService` API:

```scala
trait WeatherService:
    def forecast(): String
```

If we do not have access to the implementation source code, there is no way to discern what effects are needed at compile time.
The only way to figure it out is to run the code and see what happens.


```scala
ClosedSourceWeatherService().forecast()
// READ GPS SIGNAL
// NETWORK CALL
// res0: String = "Sunny"
```

It is possible that we are using entirely open-source or in-house code throughout our entire application.
That means that we could theoretically dig into every function involved in a complex path and note every effect.

In practice this quickly becomes impossible.

```scala
object OpenSourceLibrary:
    def submitDataToExternalService(
        payload: String
    ): Unit =
      println(s"NETWORK: Sending payload")
      saveUserInfo(payload)

    private def saveUserInfo(
        userData: String
    ): Unit =
      DataAnalytics.recordKeyDemographics(userData)
      println(s"DATABASE: Saving data")
      
object DataAnalytics:
    def recordKeyDemographics(
        userData: String
    ): Unit =
      println(s"LOGGER: Key demographic found")
```


```scala
def logic(): Unit =
    // ...Other calls...
    OpenSourceLibrary.submitDataToExternalService("Network Payload")
    // ...Other calls...
    
logic()
// NETWORK: Sending payload
// LOGGER: Key demographic found
// DATABASE: Saving data
```

Here our simple program performs 3 very different side-effects, but everything is boiled down to the same `Unit` type.
If we extrapolate this is to a production application with hundreds and thousands of functions, it is overwhelming.

Ideally, we could leverage the type system and the compiler to track the requirements for arbitrarily complex pieces of code.