Consider a simple function

```scala
def saveInformation(info: String): Unit = ???
```

If we consider only the types, this function is an `Any=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we it, we know that *some* type of side-effect is being performed, but without any specificity.


It is possible that we are using entirely open-source or in-house code throughout our entire application.
That means that we could theoretically dig into every function involved in a complex path and track every `Console`
interaction.

In practice this quickly becomes impossible.

```scala
def crunchNumbersAndPrintResult(
    x: Int,
    y: Int
): Unit = ???

def longRunningNetworkSubmission(
    payload: String
): Unit = ???

def saveUserInfoToDatabase(
    userData: String
): Unit = ???

def application(): Unit =
  crunchNumbersAndPrintResult(3, 5)
  longRunningNetworkSubmission("Network Payload")
  saveUserInfoToDatabase("User Info")
```

Here our simple program performs 3 very different side-effects, but everything is boiled down to the same `Unit` type.
If we extrapolate this is to a production application with hundreds and thousands of functions, it is overwhelming.
