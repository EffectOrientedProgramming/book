# Monads

> A function can take any number of inputs, but it can only return a single result.

We often need to convey more information than can fit into a simple result.
The programmer is forced to use side effects to express all the outcomes of a function call.
Side effects produce unpredictable results and an unpredictable program is unreliable.

The problem is that a single simple result is *too* simple.
What we need is a complex result capable of holding all necessary information that comes out of a function call.

To solve the problem we put all that information into a box: the original result together with any extra information (such as error conditions).
We return that box from the function.

Now we've got boxes everywhere, and programming becomes quite messy and complicated.
Every time you call a function, you must unpack and analyze the contents of the box that comes out as the result.
If there's a problem, you must handle it right after the function is called, which is awkward and often produces duplicate code.
People probably won't use our system unless we figure out a way to simplify and automate the use of these boxes.

What if we had a standard set of operations that work on all boxes, to make our system easy to use by eliminating all that duplicated code?
The box---and these associated operations---is a monad.

## Concepts Used in this Chapter

### General Programming

- Sub-typing
- If/else
- String interpolation
- List
  - Applying functions to elements of a list via `foreach`

### Scala Specific

- case classes
- Type comes after `identifier:`
- Functions
  - Last line is the result
- Vals
- Everything is an expression
- Significant Indention / End Marker
- Match / cases
- Destructuring through unapply
- How our examples work (MDoc)
  - Result shows on next line
  - Console output shows as following comments

## The Error Monad

Initially, the most compelling reason to use monads is error handling.

Encountering an error during a function call generally means two things:

1. You can't continue executing the function in the normal fashion.

2. You can't return a normal result.

Many languages use *exceptions* for handling errors.
An exception *throws* out of the current execution path to locate a user-written *handler* to deal with the error.
There are two goals for exceptions:

1. Separate error-handling code from "success-path" code, so the success-path code is easier to understand and reason about.

2. Reduce redundant error-handling code by handling associated errors in a single place.

What if we make a box called `Result` containing *both* the success-path value together with error information if it fails?
For simplicity, both the error information and the success data are `String`s:

```scala mdoc
case class Fail(why: String)     extends Result
case class Success(data: String) extends Result
```

If you reach a point in a function where something goes wrong, you return a `Fail` with failure information stored in `why`.
If you get all the way through the function without any failures, you return a `Success` with the return calculation stored in `data`.

The Scala `for` comprehension is designed to work with monads.
The `<-` in a `for` comprehension *automatically checks and unpacks a monad!*
The monad does not have to be a standard or built-in type; you can write one yourself as we've done with `Result` (whose definition is shown later in this chapter, after you understand what it needs to do).
Let's see how `Result` works:

```scala mdoc
def check(
    step: String,
    stop: String,
    history: String
): Result =
  val result =
    if step == stop then
      Fail(history + step)
    else
      Success(history + step)
  println(s"check($step, $stop): $result")
  result
```

`check` compares `step` to `stop`.
If they're equal, it returns a `Fail` object, otherwise it returns a `Success` object.

```scala mdoc
def compose(stop: String): Result =
  for
    a: String <- check("a", stop, "")
    b: String <- check("b", stop, a)
    c: String <- check("c", stop, b)
  yield
    println(s"Yielding: $c + d")
    c + "d"
```

`compose` takes `stop` indicating how far we want to get through the execution of `compose` before it fails.

The `for` comprehension attempts to execute three calls to `check`, each of which takes the next value of `step` in alphabetic succession.
Each expression uses the backwards-arrow `<-` to assign the result to a `String` value.
That value is passed to `check` in the subsequent expression in the comprehension.
If all three expressions execute successfully, the `yield` expression uses `c` to produce the final `Result` value which is returned from the function.

What happens if a call to `check` fails?
We'll call `compose` with successive values of `stop` from `"a"` to `"d"`:

```scala mdoc
compose("a")
```

`check("a", stop, "")` immediately fails when `stop = "a"`, so the result returned from `check` is `Fail(a)`.

Here's where things get especially interesting.
When Scala sees `<-` in a `for` comprehension, it automatically calls `flatMap`.
So `flatMap` is called on the result of of `check("a", stop, "")`.
That result is `Fail` and *no further lines in `compose` are executed*.
The `a` to the left of the `<-` is never initialized, nor are `b` or `c`.
The resulting value of `compose` becomes the value returned by `flatMap`, which is `Fail(a)`.

The `Result` returned by `compose` can be checked for failure, and error-handling can be executed if `Fail` is found.
All the error-handling for `compose` can be in one place, in the same way that a `catch` clause combines error-handling code.

```scala mdoc
compose("b")
```

With `stop = "b"`, the first expression in the `for` comprehension is now successful.
The value of `a` is successfully assigned, then passed into `check("b", stop, a)` in the second expression.
Now the second expression fails and the resulting value of `compose` becomes `Fail(ab)`.

```scala mdoc
compose("c")
```

Now we get all the way to the third expression in the `for` comprehension before it fails.
But notice that in this case `map` is called rather than `flatMap`.
The last `<-` in a `for` comprehension calls `map` instead of `flatMap`, for reasons that will become clear.

Finally, `stop = "d"` successfully makes it through the entire initialization for `compose`:

```scala mdoc
compose("d")
```

The return value of `check("c", stop, b)` is `Success(abc)` and this is used to initialize `c`.

The `yield` expression produces the final result returned by `compose`.
You should find all potential problems by the time you reach `yield`, so the `yield` expression should not be able to fail.
Note that `c` is of type `String` but `compose` is of type `Result`.
The `yield` expression is automatically wrapped in a `Success` object.

We `compose` a result from multiple expressions and the whole `for` comprehension will either succeed or fail.

One way to discover the `Fail` case is to use a pattern match. Here, we extract the `why` in `Fail` and the `data` in `Success` to use in the corresponding `println` statements:

```scala mdoc
compose("a") match
  case Fail(why) =>
    println(s"Error-handling for $why")
  case Success(data) =>
    println(s"Handling success value: $data")
```

`case Fail` becomes the equivalent of the `catch` clause in exception handling, so all the error handling for `compose` is now isolated in one place, just as in a `catch` clause.

You can see in the output from the various calls to `compose` that the compiler responds to a `<-` within a `for` comprehension by calling `flatMap` or `map`.
Thus, it looks like our `Result` must have `flatMap` and `map` methods in order to allow these calls.
Here's the definition of `Result`:

```scala mdoc
trait Result:
  def flatMap(f: String => Result): Result =
    println(s"flatMap on $this")
    this match
      case fail: Fail =>
        fail
      case Success(c) =>
        f(c)

  def map(f: String => String): Result =
    println(s"map on $this")
    this match
      case fail: Fail =>
        fail
      case Success(c) =>
        Success(f(c))

end Result
```

The code in the two methods is almost identical.
Each receives a function `f` as an argument.
Each checks the subtype of the current (`Result`) object.
A `Fail` just returns that `Fail` object, and never calls `f`.
Only a `Success` causes `f` to be evaluated.
In `flatMap`, `f` is called on the contents of the `Success`.
In `map`, `f` is also called on the contents of the `Success`, and then the result of that call is wrapped in another `Success` object.

## Predefined Monads

Because the `for` comprehension provides direct support for monads, you might not be surprised to discover that Scala comes with some predefined monads.
The two most common of these are `Either` and `Option`.
These are generic so they work with any type.

`Either` looks just like our `Result` monad but with different names.
People commonly use `Either` to produce the same effect as `Result`.
Our `Fail` becomes `Left` in `Either`, and our `Success` becomes `Right`.
`Either` has numerous additional methods beyond `map` and `flatMap`, so it is much more full-featured.

X> **Exercise 1:** Modify `ShowResult` to use `Either` instead of `Result`.
X> Your output should look like this:

```scala mdoc:invisible
def echeck(
    step: String,
    stop: String,
    msg: String
) =
  val result =
    if step == stop then
      Left(msg + step)
    else
      Right(msg + step)
  println(s"check($step): $result")
  result

def solution1(stop: String) =
  println(s">> compose($stop) <<")

  val compose =
    for
      a: String <- echeck("a", stop, "")
      b: String <- echeck("b", stop, a)
      c: String <- echeck("c", stop, b)
    yield
      println(s"Completed: $c")
      c

  println(compose)
  // Using Either's left-projection:
  for (failure <- compose.left)
    println(s"Error-handling for $failure")

end solution1
```

```scala mdoc
List("a", "b", "c", "d").foreach(solution1)
```

X> **Exercise 2:** Modify the solution to Exercise 1 to work with `Int` instead of `String`.
X> Change `msg` in the `check` argument list to `i`, an `Int`.
X> Your output should look like this:

```scala mdoc:invisible
def icheck(
    step: String,
    stop: String,
    i: String
) =
  val result =
    if step == stop then
      Left(i + step)
    else
      Right(i + step)
  println(s"check($step): $result")
  result
end check

def solution2(stop: String) =
  println(s">> compose($stop) <<")

  val compose =
    for
      a: String <- icheck("a", stop, "")
      b: String <- icheck("b", stop, a)
      c: String <- icheck("c", stop, b)
    yield
      println(s"Completed: $c")
      c

  println(compose);
  for (failure <- compose.left)
    println(s"Error-handling for $failure")

end solution2
```

```scala mdoc
List("a", "b", "c", "d").foreach(solution2)
```

`Option` is like `Either` except that the `Right`-side (success) case becomes `Some` (that is, it has a value) and the `Left`-side (failure) case becomes `None`.
`None` simply means that there is no value, which isn't necessarily an error.
For example, if you look something up in a `Map`, there might not be a value for your key, so returning an `Option` of `None` is a common and reasonable result.

X> **Exercise 3:** Modify `ShowResult` to work with `Option` instead of `Result`.
X> Your output should look like this:

```scala mdoc:invisible
def ocheck(step: Int, stop: Int, msg: String) =
  val result =
    if step == stop then
      None
    else
      Some(msg + step)
  println(s"check($step): $result")
  result
end check

def solution3(stop: Int) =
  println(s">> compose($stop) <<")

  val compose =
    for
      a: String <- ocheck(1, stop, "")
      b: String <- ocheck(2, stop, a)
      c: String <- ocheck(3, stop, b)
    yield
      println(s"Completed: $c")
      c

  println(compose);
  // TODO Consider how/where to show this.
  //   Just return the for-comp result directly
  if compose == None then
    println(s"Error-handling for None")

end solution3
```

```scala mdoc
List(1, 2, 3, 4).foreach(solution3)
```

X> **Exercise 4:** Modify `Result` so it is an `enum` instead of a `trait`.
X> Modify `ShowResult` to demonstrate this new `enum ResultEnum`.
X> Your output should look like this:

```scala mdoc:invisible
enum ResultEnum:
  def flatMap(
      f: String => ResultEnum
  ): ResultEnum =
    println(s"flatMap on $this")
    this match
      case SuccessRE(c) =>
        f(c)
      case fail: FailRE =>
        fail

  def map(f: String => String): ResultEnum =
    println(s"map on $this")
    this match
      case SuccessRE(c) =>
        SuccessRE(f(c))
      case fail: FailRE =>
        fail

  case FailRE(why: String)
  case SuccessRE(data: String)

end ResultEnum
```

```scala mdoc:invisible
import ResultEnum.*

def checkRE(
    step: Int,
    stop: Int,
    msg: String
): ResultEnum =
  val result =
    if step == stop then
      FailRE(msg + step)
    else
      SuccessRE(msg + step)
  println(s"check($step): $result")
  result

def solution4(stop: Int) =
  for
    a: String <- checkRE(1, stop, "")
    b: String <- checkRE(2, stop, a)
    c: String <- checkRE(3, stop, b)
  yield
    println(s"Completed: $c")
    c.toUpperCase.nn
```

```scala mdoc
List(1, 2, 3, 4).foreach(solution4)
```

## Understanding the `for` Comprehension

At this point you should have a sense of what a `for` comprehension is doing, but *how* it does it is still a bit mysterious.
We can use the `Either` predefined monad to understand it better.
Here, each expression in `fc1` uses `Right`, so each one can never fail.
This doesn't matter because we just want to look at the structure of the code:

```scala mdoc
val fc1 =
  for
    a <- Right("A")
    b <- Right("B")
    c <- Right("C")
  yield s"Result: $a $b $c"
```

Because we never created a `Left`, Scala decided that the `Left` type should be `Nothing`.

IntelliJ IDEA provides a nice tool that expands this comprehension to show the calls to `flatMap` and `map`.
If you select the `for`, you'll see a little light bulb appear.
Click on that and select "Desugar for comprehension."
The result looks like this:

```scala mdoc
val fc2 =
  Right("A").flatMap(a =>
    Right("B").flatMap(b =>
      Right("C").map(c => s"Result: $a $b $c")
    )
  )
```

The `for` comprehension left-arrow `<-` generates a call to `flatMap`.
Notice that the argument to `flatMap` is a function.

Now look back at `flatMap` in `Result`.
In the `Fail` case (which is `Left` for `Either`), `flatMap` just returns the `Fail` object and *doesn't call that function.*
Here, in the `Right` case (which is like `Success` for `Result`), the function is called and produces `Right("B")` ... with another call to `flatMap`.
This `flatMap` argument is another function, which is again not called in the `Left` case.
In the `Right` case, it returns `Right("C")` ... with another call, but this time to `map`.
The argument to `map` is another function, again not called in the `Left` case.
In the `Right` case, it returns something different: the `yield` expression.
Also, `map` wraps the `yield` expression in a `Right`, unlike `flatMap`.

Because of this cascade of functions within functions, any `flatMap` or `map` called on a `Left` result *will not evaluate the rest of the cascade.*
It stops the evaluation and returns `Left` at that point, and the `Left` becomes the result of the expression.
This cascaded expression is thus only evaluated up to the point where a `Left` first appears.
The rest of the expression can be thought of as being short-circuited at that point.

There's another benefit of this cascade of function calls: `a`, `b` and `c` are all in scope by the time you reach the `yield` expression that is the `map` argument.

X> **Exercise 5:** Modify `fc1` to use `Some` instead of `Either`.
X> Verify it works, then produce the "desugared" version as you see with `fc2`.
X> Your output should look like this:

```scala mdoc:invisible
val solution5a =
  for
    a <- Some("A")
    b <- Some("B")
    c <- Some("C")
  yield s"Result: $a $b $c"

val solution5b =
  Some("A").flatMap(a =>
    Some("B").flatMap(b =>
      Some("C").map(c => s"Result: $a $b $c")
    )
  )
```

```scala mdoc
solution5a
solution5b
```

## Summary

Think back to the first time you grasped the way dynamic binding produces virtual function behavior.
How you realized that this pattern is so important that it is directly implemented by the compiler.
You probably had an insight that the pattern of inheritance polymorphism is fundamental to object-oriented programming.

In this chapter you've experienced a similar realization, but for functional programming.
Producing result information in a monad is so fundamental to functional programming that the Scala compiler provides direct support for this pattern in the form of the `for` comprehension.

Although `for` has multiple forms in Scala, we will primarily use the form shown in this chapter throughout the book.
