# Console

This is generally the first effect that we will want as we learn to construct functional programs.
It is so basic that most languages do not consider it as anything special.
The typical first scala program is something like:

```scala
println("Hi there.")
// Hi there.
```

Simple enough, and familiar to anyone that has programmed before.
Take a look at the signature of this function in the Scala `Predef` object:

```scala
def println(x: Any): Unit = ???
```

If we consider only the types, this function is an `Any=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we it, we know that *some* type of side-effect is being performed, but without any specificity.

TODO Consider separate section on `Unit`

If we check the implementation, we discover this:

```scala
def println(x: Any): Unit = Console.println(x)
```

Now it is clear that we are printing to the `Console`.
If we do not have access to the implementation source code, this is a surprise to us at runtime.

*TODO This is a pretty large, general tangent about effects.
It might belong in an earlier chapter.*

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
