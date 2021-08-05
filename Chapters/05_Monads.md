# Monads

> A function can take any number of inputs, but it can only return a single result.

We often need to convey more information than can fit into a simple result.
The programmer is forced to use side effects to express all the outcomes of a function call.
Side effects produce unpredictable results and an unpredictable program is unreliable.

The problem is that a single simple result is *too* simple.
What we need is a complex result capable of holding all necessary information that comes out of a function call.

To solve the problem we put all that extra result information, along with the original result, into a box.
We return that box from the function.

Now we've got boxes everywhere, and programming becomes quite messy and complicated.
Every time you call a function, you must unpack and analyze the contents of the box that comes out as the result.
If there's a problem, you must handle it right after the function is called, which is awkward and often produces duplicate code.
People probably won't use our system unless we figure out a way to simplify and automate the manipulation of these boxes.

What if we had a standard set of operations that work on all boxes, to automate the use of our system and eliminate all that duplicated code?
The box---and the associated operations---is a monad.

## The Error Monad

Programmers have always had to deal with errors, so this makes a good initial use-case for monads.

Encountering an error during a function call generally means two things:

1. You can't continue executing the function in the normal fashion.

2. You can't return a normal result.

Many languages use *exceptions* for handling errors.
An exception *throws* out of the current execution path to locate a user-written *handler* to deal with the error.
There are two goals for exceptions:

1. Separate error-handling code from "success-path" code, so the success-path code is easier to understand and reason about.

2. Reduce redundant error-handling code by handling associated errors in a single place.

TODO: Need better insights on the problems with exceptions
The problem with exceptions is that they lose important context information when they are thrown.
Also it separates "normal failure" from "exceptional failure" (Map get() example).

What if we make a box called `Result` containing *both* the success-path result together with error information if it fails?
For simplicity, both the error information and the success data are `String`s:

```scala mdoc
case class Fail(why: String)     extends Result
case class Success(data: String) extends Result
```

If you reach a point in a function where something goes wrong, you return a `Fail Result` with failure information stored in `why`.
If you get all the way through the function without any failures, you return a `Success Result` with the return calculation stored in `data`.

The Scala `for`-comprehension is designed to work with monads.
The `<-` in a comprehension *automatically checks and unpacks a monad!*
The monad does not have to be a standard or built-in type; you can write one yourself as we've done with `Result`.
Let's see how it works:

```scala mdoc
// Monads/ShowResult.scala

def show(n: Char) =
  def op(id: Char, msg: String): Result =
    val result =
      if n == id then
        Fail(msg + id.toString)
      else
        Success(msg + id.toString)
    println(s"op($id): $result")
    result
  end op

  val compose: Result =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c.toUpperCase.nn

  println(compose)
  compose match
    case Fail(why) =>
      println(s"Error-handling for $why")
    case Success(data) =>
      println("Success: " + data)
end show
```

`show()` takes `n: Char` indicating how far we want to get through the execution of `compose` before it fails.
Note that `n` is in scope within the nested function `op()`.
`op()` compares `n` to its `id` argument and if they're equal it returns a `Fail` object, otherwise it returns a `Success` object.

The `for`-comprehension within `compose` attempts to execute three calls to `op()`, each of which has a successive `id`.
Each expression uses the backwards-arrow `<-` to assign the result to a `String` value.
That value is passed to `op()` in the subsequent expression in the comprehension.
If all three expressions execute successfully, the `yield` expression uses `c` to produce the final `Result`.

But what happens if a call to `op()` fails?
We'll call `show()` with successive values of `n` from `'a'` to `'d'`:

```scala mdoc
show('a')
```

`op('a', "")` immediately fails when `n = 'a'`, so the result returned from `op()` is `Fail(a)`.
The `<-` calls `flatMap()` on that result, *but no further lines in `compose` are executed*.
The execution stops and the resulting value of `compose` becomes `Fail(a)`.
The last lines in `show()` check for failure and execute error-handling code if `Fail` is found.
This is the equivalent of the `catch` clause in exception handling, so all the error-handling for `compose` is now in one place.

```scala mdoc
show('b')
```

With `n = 'b'`, the first expression in the `for` comprehension is now successful.
The value of `a` is successfully assigned, then passed into `op('b', a)` in the second expression.
Now the second expression fails and the resulting value of `compose` becomes `Fail(ab)`.
Once again we end up in the error-handling code.

```scala mdoc
show('c')
```

Now we get all the way to the third expression in the `for` comprehension before it fails.
But notice that in this case `map()` is called rather than `flatMap()`.
The last `<-` in a `for` comprehension calls `map()` instead of `flatMap()`, for reasons that will become clear.

Finally, `n = 'd'` successfully makes it through the entire initialization for `compose`:

```scala mdoc
show('d')
```

When `map()` is called on the result of `op('c', b)`, the return value of `map()` is used to initialize `c`.
The `yield` expression produces the final result that is assigned to `compose`.
You should find all potential problems by the time you reach `yield`, so the `yield` expression should not be able to fail.
Note that `c` is of type `String` but `compose` is of type `Result`.
The `yield` expression automatically wraps `c` in a `Success` object.
{{ What mechanism wraps the `yield` expression? }}

The identifier name for `val compose` is intentional.
We are composing a result from multiple expressions and the whole `for` comprehension will either succeed or fail.

We now know that, for our type to be automatically unpacked by the `<-` within a `for` comprehension, it must have a `map()` and a `flatMap()`.
Here's the full definition of `Result`:

```scala mdoc
// Monads/Result.scala

trait Result:
  def flatMap(f: String => Result): Result =
    println(s"flatMap() on $this")
    this.match
      case Success(c) =>
        f(c)
      case fail: Fail =>
        fail

  def map(f: String => String): Result =
    println(s"map() on $this")
    this.match
      case Success(c) =>
        Success(f(c))
      case fail: Fail =>
        fail

end Result
```

{{ Explanation }}

## Predefined Monads

Because the `for` comprehension provides direct support for monads, you might not be surprised to discover that Scala comes with some predefined monads.
The two most common of these are `Either` and `Option`.

`Either` looks just like our `Result` monad; it just uses different names.
People commonly use `Either` to produce the same effect.

X> Modify `ShowResult.scala` to use `Either` instead of `Result`.
X> Your output should look like this:

```scala mdoc:invisible
// Monads/Solution1.scala
// package monads

def eshow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, msg: String) =
    val result =
      if n == id then
        Left(msg + id.toString)
      else
        Right(msg + id.toString)
    println(s"op($id): $result")
    result
  end op

  val compose =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c

  println(compose)
  // Using Either's left-projection:
  for (failure <- compose.left)
    println(s"Error-handling for $failure")

end eshow
```

```scala mdoc
'a' to 'd' foreach eshow
```

- Exercise: modify ShowResult.scala to work with `Option`
- Exercise: show that GenericResult.scala works with ShowResult.scala
- Exercise: Modify Above solution to work with `Int` instead of `String`
- Exercise: Modify GenericResult.scala to create GenericOption.scala, implementing your own version of `Option`
