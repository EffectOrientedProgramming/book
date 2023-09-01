# Cause TODO Consider putting inside error handling

`Cause` will track all errors originating from a single call in an application, regardless of concurrency and parallelism.

```scala mdoc:silent
val logic =
  ZIO
    .die(new Exception("Connection lost"))
    .ensuring(
      ZIO.die(
        throw new Exception("Release Failed")
      )
    )
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
val thrownLogic =
  ZIO.attempt(
    try
      throw new Exception(
        "Client connection lost"
      )
    finally
      try () // Cleanup
      finally
        throw new Exception("Release Failed")
  )
runDemo(thrownLogic)
```

We will only see the later `pool` problem.
If we throw an `Exception` in our logic, and then throw another while cleaning up, we simply lose the original.
This is because thrown `Exception`s cannot be _composed_.

In a language that cannot `throw`, following the execution path is simple, following 2 basic rules:

    - At a branch, execute only the first match
    - Otherwise, Read everything from left-to-right, top-to-bottom, 

Once you add `throw`, the rules are more complicated

    - At a branch, execute only the first match
    - Otherwise, Read everything from left-to-right, top-to-bottom,
    - Unless we `throw`, which means immediately jumping through a different dimension away from the code you're viewing

### Linear reporting
Everything must be reported linearly, even in systems that are executing on different fibers, across several threads, amongst multiple cores.
