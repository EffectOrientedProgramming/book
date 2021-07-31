# Unit

Consider a simple function

```scala mdoc
def saveInformation(info: Any): Unit = ???
```

If we consider only the types, this function is an `Any=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we see it, we know that *some* type of side-effect is being performed, but without any specificity.

If we do not have access to the implementation source code, there is no way to discern what effects are needed/performed at compile time.

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
```

```scala mdoc:invisible
class ClosedSourceCodeImpl() extends LibraryCode:
    def crunchNumbersAndPrintResult(
        x: Int,
        y: Int
    ): Unit =
      println(s"CONSOLE: Result of crunching $x and $y")

    def longRunningNetworkSubmission(
        payload: String
    ): Unit =
      println(s"NETWORK: Sending payload")

    def saveUserInfoToDatabase(
        userData: String
    ): Unit =
      println(s"DATABASE: Saving data")
```


```scala mdoc
def logic(): Unit =
    val library: LibraryCode = ClosedSourceCodeImpl()
    library.crunchNumbersAndPrintResult(3, 5)
    library.longRunningNetworkSubmission("Network Payload")
    library.saveUserInfoToDatabase("User Info")
    
logic()
```

Here our simple program performs 3 very different side-effects, but everything is boiled down to the same `Unit` type.
If we extrapolate this is to a production application with hundreds and thousands of functions, it is overwhelming.

It is possible that we are using entirely open-source or in-house code throughout our entire application.
That means that we could theoretically dig into every function involved in a complex path and note every effect.

In practice this quickly becomes impossible.

