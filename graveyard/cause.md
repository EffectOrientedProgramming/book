`Cause` will track all errors originating from a single call in an application, regardless of concurrency and parallelism.

```scala mdoc:silent
val logic =
  ZIO
    .die:
      Exception:
        "Connection lost"
    .ensuring:
      ZIO.die:
        throw Exception:
          "Release Failed"
```
```scala mdoc
runDemo(logic)
```

Cause allows you to aggregate multiple errors of the same type

`&&`/`Both` represents parallel failures
`++`/`Then` represents sequential failures

Cause.die will show you the line that failed, because it requires a throwable
Cause.fail will not necessarily, because it can be any arbitrary type

## Manual demonstration of these operators

```scala mdoc
runDemo(
  Console.printLine(
    (
      Cause.die(Exception("1")) ++
        (Cause.fail(Exception("2a")) &&
          Cause.fail(Exception("2b"))) ++
        Cause
          .stackless(Cause.fail(Exception("3")))
    ).prettyPrint
  )
)
```

## Avoided Technique - Throwing Exceptions

Now we will highlight the deficiencies of throwing `Exception`s.
The previous code might be written in this style:

```scala mdoc
runDemo:
  ZIO.attempt:
    try
      throw Exception:
        "Client connection lost"
    finally
      throw Exception:
        "Release Failed"
```

We will only see the later `pool` problem.
If we throw an `Exception` in our logic, and then throw another while cleaning up, we simply lose the original.
This is because thrown `Exception`s cannot be _composed_.

