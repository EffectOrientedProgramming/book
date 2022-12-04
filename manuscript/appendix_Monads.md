# Monads

> A function can take any number of inputs, but it can only return a single result.

We often need a function to produce more information than can fit into a simple result.
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
Mathematicians often say, *A monad embeds an object into an object with a richer structure*.[^fn1]

## The Error Monad

Initially, the most compelling reason to use monads is error handling.

What if we make a `Box` containing *both* the success-path value together with error information if it fails?
For simplicity, both the error information and the success data are `String`s:

```scala
case class Fail(why: String)     extends Box
case class Success(data: String) extends Box
```

If you reach a point in a function where something goes wrong, you return a `Fail` with failure information stored in `why`.
If you get all the way through the function without any failures, you return a `Success` with the return calculation stored in `data`.

The Scala `for` comprehension is designed to work with monads.
The `<-` in a `for` comprehension *automatically checks and unpacks a monad!*
The monad does not have to be a standard or built-in type; you can write one yourself as we've done with `Box` (whose definition is shown later in this chapter, after you understand what it needs to do).
Let's see how `Box` works:

```scala
def check(
    step: String,
    stop: String,
    history: String
): Box =
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

```scala
def compose(stop: String): Box =
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
If all three expressions execute successfully, the `yield` expression uses `c` to produce the final `Box` value which is returned from the function.

What happens if a call to `check` fails?
We'll call `compose` with successive values of `stop` from `"a"` to `"d"`:

```scala
compose("a")
// check(a, a): Fail(a)
// flatMap on Fail(a)
// res0: Box = Fail(why = "a")
```

`check("a", stop, "")` immediately fails when `stop = "a"`, so the result returned from `check` is `Fail(a)`.

Here's where things get especially interesting.
When Scala sees `<-` in a `for` comprehension, it automatically calls `flatMap`.
So `flatMap` is called on the result of of `check("a", stop, "")`.
That result is `Fail` and *no further lines in `compose` are executed*.
The `a` to the left of the `<-` is never initialized, nor are `b` or `c`.
The resulting value of `compose` becomes the value returned by `flatMap`, which is `Fail(a)`.

The `Box` returned by `compose` can be checked for failure, and error-handling can be executed if `Fail` is found.
All the error-handling for `compose` can be in one place, in the same way that a `catch` clause combines error-handling code.

```scala
compose("b")
// check(a, b): Success(a)
// flatMap on Success(a)
// check(b, b): Fail(ab)
// flatMap on Fail(ab)
// res1: Box = Fail(why = "ab")
```

With `stop = "b"`, the first expression in the `for` comprehension is now successful.
The value of `a` is successfully assigned, then passed into `check("b", stop, a)` in the second expression.
Now the second expression fails and the resulting value of `compose` becomes `Fail(ab)`.

```scala
compose("c")
// check(a, c): Success(a)
// flatMap on Success(a)
// check(b, c): Success(ab)
// flatMap on Success(ab)
// check(c, c): Fail(abc)
// map on Fail(abc)
// res2: Box = Fail(why = "abc")
```

Now we get all the way to the third expression in the `for` comprehension before it fails.
But notice that in this case `map` is called rather than `flatMap`.
The last `<-` in a `for` comprehension calls `map` instead of `flatMap`, for reasons we explain shortly.

Finally, `stop = "d"` successfully makes it through the entire initialization for `compose`:

```scala
compose("d")
// check(a, d): Success(a)
// flatMap on Success(a)
// check(b, d): Success(ab)
// flatMap on Success(ab)
// check(c, d): Success(abc)
// map on Success(abc)
// Yielding: abc + d
// res3: Box = Success(data = "abcd")
```

The return value of `check("c", stop, b)` is `Success(abc)` and this is used to initialize `c`.

The `yield` expression produces the final result returned by `compose`.
You should find all potential problems by the time you reach `yield`, so the `yield` expression should not be able to fail.
Note that `c` is of type `String` but `compose` is of type `Box`.
The `yield` expression is automatically wrapped in a `Success` object.

We `compose` a result from multiple expressions and the whole `for` comprehension will either succeed or fail.

One way to discover the `Fail` case is to use a pattern match. Here, we extract the `why` in `Fail` and the `data` in `Success` to use in the corresponding `println` statements:

```scala
compose("a") match
  case Fail(why) =>
    println(s"Error-handling for $why")
  case Success(data) =>
    println(s"Handling success value: $data")
// check(a, a): Fail(a)
// flatMap on Fail(a)
// Error-handling for a
```

`case Fail` becomes the equivalent of the `catch` clause in exception handling, so all the error handling for `compose` is now isolated in one place, just as in a `catch` clause.

You can see in the output from the various calls to `compose` that the compiler responds to a `<-` within a `for` comprehension by calling `flatMap` or `map`.
Thus, it looks like our `Box` must have `flatMap` and `map` methods in order to allow these calls.
Here's the definition of `Box`:

```scala
trait Box:
  def flatMap(f: String => Box): Box =
    println(s"flatMap on $this")
    this match
      case fail: Fail =>
        fail
      case Success(c) =>
        f(c)

  def map(f: String => String): Box =
    println(s"map on $this")
    this match
      case fail: Fail =>
        fail
      case Success(c) =>
        Success(f(c))
```

The code in the two methods is almost identical.
Each receives a function `f` as an argument.
Each checks the subtype of the current (`Box`) object.
A `Fail` just returns that `Fail` object, and never calls `f`.
Only a `Success` causes `f` to be evaluated.
In `flatMap`, `f` is called on the contents of the `Success`.
In `map`, `f` is also called on the contents of the `Success`, and then the result of that call is wrapped in another `Success` object.

You can also think of a monad as some functions that enable you to separate the steps of a computation from what happens between those steps.

## Predefined Monads

Because the `for` comprehension provides direct support for monads, you might not be surprised to discover that Scala comes with some predefined monads.
The two most common of these are `Either` and `Option`.
These are generic so they work with any type.

TODO {{This explanation is confusing}}
TODO {{Convey that it holds generic types}}
People commonly use `Either` to produce the same effect as `Box`.
The `Fail` in `Box` becomes `Left` in `Either`, and the `Success` in `Box` becomes `Right` in `Either`.
`Either` has numerous additional methods beyond `map` and `flatMap`, so it is much more full-featured.

X> **Exercise 1:** Modify `ShowBox` {{From where???}} to use `Either` instead of `Box`.
X> Your output should look like this:


```scala
List("a", "b", "c", "d").foreach(solution1)
// >> compose(a) <<
// check(a): Left(a)
// Left(a)
// Error-handling for a
// >> compose(b) <<
// check(a): Right(a)
// check(b): Left(ab)
// Left(ab)
// Error-handling for ab
// >> compose(c) <<
// check(a): Right(a)
// check(b): Right(ab)
// check(c): Left(abc)
// Left(abc)
// Error-handling for abc
// >> compose(d) <<
// check(a): Right(a)
// check(b): Right(ab)
// check(c): Right(abc)
// Completed: abc
// Right(abc)
```

X> **Exercise 2:** Modify the solution to Exercise 1 to work with `Int` instead of `String`.
X> Change `msg` in the `check` argument list to `i`, an `Int`.
X> Your output should look like this:


```scala
List("a", "b", "c", "d").foreach(solution2)
// >> compose(a) <<
// check(a): Left(a)
// Left(a)
// Error-handling for a
// >> compose(b) <<
// check(a): Right(a)
// check(b): Left(ab)
// Left(ab)
// Error-handling for ab
// >> compose(c) <<
// check(a): Right(a)
// check(b): Right(ab)
// check(c): Left(abc)
// Left(abc)
// Error-handling for abc
// >> compose(d) <<
// check(a): Right(a)
// check(b): Right(ab)
// check(c): Right(abc)
// Completed: abc
// Right(abc)
```

`Option` is like `Either` except that the `Right`-side (success) case becomes `Some` (that is, it has a value) and the `Left`-side (failure) case becomes `None`.
`None` simply means that there is no value, which isn't necessarily an error.
For example, if you look something up in a `Map`, there might not be a value for your key, so returning an `Option` of `None` is a common and reasonable result.

X> **Exercise 3:** Modify `ShowBox` to work with `Option` instead of `Box`.
X> Your output should look like this:


```scala
List(1, 2, 3, 4).foreach(solution3)
// >> compose(1) <<
// check(1): None
// None
// Error-handling for None
// >> compose(2) <<
// check(1): Some(1)
// check(2): None
// None
// Error-handling for None
// >> compose(3) <<
// check(1): Some(1)
// check(2): Some(12)
// check(3): None
// None
// Error-handling for None
// >> compose(4) <<
// check(1): Some(1)
// check(2): Some(12)
// check(3): Some(123)
// Completed: 123
// Some(123)
```

X> **Exercise 4:** Modify `Box` so it is an `enum` instead of a `trait`.
X> Modify `ShowBox` to demonstrate this new `enum BoxEnum`.
X> Your output should look like this:



```scala
List(1, 2, 3, 4).foreach(solution4)
// check(1): FailRE(1)
// flatMap on FailRE(1)
// check(1): SuccessRE(1)
// flatMap on SuccessRE(1)
// check(2): FailRE(12)
// flatMap on FailRE(12)
// check(1): SuccessRE(1)
// flatMap on SuccessRE(1)
// check(2): SuccessRE(12)
// flatMap on SuccessRE(12)
// check(3): FailRE(123)
// map on FailRE(123)
// check(1): SuccessRE(1)
// flatMap on SuccessRE(1)
// check(2): SuccessRE(12)
// flatMap on SuccessRE(12)
// check(3): SuccessRE(123)
// map on SuccessRE(123)
// Completed: 123
```

## Understanding the `for` Comprehension

At this point you should have a sense of what a `for` comprehension is doing, but *how* it does this is still mysterious.
We can use the `Either` monad to understand it better.
Here, each expression in `fc1` uses `Right`, so each one can never fail.
This doesn't matter because we just want to look at the structure of the code:

```scala
val fc1 =
  for
    a <- Right("A")
    b <- Right("B")
    c <- Right("C")
  yield s"Box: $a $b $c"
// fc1: Either[Nothing, String] = Right(value = "Box: A B C")
```

Because we never created a `Left`, Scala decided that the `Left` type should be `Nothing`.

IntelliJ IDEA provides a nice tool that expands this comprehension to show the calls to `flatMap` and `map`.
If you select the `for`, you'll see a little light bulb appear.
Click on that and select "Desugar for comprehension."
TODO {{Put desugaring sooner}}
The result looks like this:

```scala
val fc2 =
  Right("A").flatMap(a =>
    Right("B").flatMap(b =>
      Right("C").map(c => s"Box: $a $b $c")
    )
  )
// fc2: Either[Nothing, String] = Right(value = "Box: A B C")
```

The `for` comprehension left-arrow `<-` generates a call to `flatMap`.
Notice that the argument to `flatMap` is a function.

Now look back at `flatMap` in `Box`.
In the `Fail` case (which is `Left` for `Either`), `flatMap` just returns the `Fail` object and *doesn't call that function.*
Here, in the `Right` case (which is `Success` for `Box`), the function is called and produces `Right("B")` ... with another call to `flatMap`.
This `flatMap` argument is another function, which is again not called in the `Left` case.
In the `Right` case, it returns `Right("C")` ... with another call, but this time to `map`.
The argument to `map` is another function, again not called in the `Left` case.
In the `Right` case, it returns something different: the `yield` expression.
Also, `map` wraps the `yield` expression in a `Right`, unlike `flatMap`.

Because of this cascade of functions within functions, any `flatMap` or `map` called on a `Left` result *will not evaluate the rest of the cascade.*
It stops the evaluation and returns `Left` at that point, and the `Left` becomes the result of the expression.
This cascaded expression is thus only evaluated until a `Left` appears.
The rest of the expression can be thought of as being short-circuited at that point.

There's another benefit of this cascade of function calls: `a`, `b` and `c` are all in scope by the time you reach the `yield` expression that is the `map` argument.

X> **Exercise 5:** Modify `fc1` to use `Some` instead of `Either`.
X> Verify it works, then produce the "desugared" version as you see with `fc2`.
X> Your output should look like this:


```scala
solution5a
// res9: Option[String] = Some(value = "Box: A B C")
solution5b
// res10: Option[String] = Some(value = "Box: A B C")
```

### Future
TODO{{Demonstrate Futures using a for comprehension -- I think this belongs in its own chapter, perhaps where we introduce concurrency}}

## Summary

When I (Bruce) began tackling my first object-oriented language (C++), I became stumped, and obsessed with, the `virtual` keyword. 
This was in the early days of C++ when the language was only implemented as `cfront`, a translator that took C++ code and generated C.
That way, C++ could be compiled on any machine with a C compiler, which meant it could be quickly ported to many existing machines.

Using `cfront`, I could puzzle through the C code generated by the `virtual` keyword.
`virtual`, it turned out, only made sense for a member function in an inheritance hierarchy.
It created a table that redirected to a different implementation of that member function, depending on the specific subtype of the object.

What struck me was that the language included a special keyword for this specific mechanism.
I later discovered that some C programmers had been creating similar tables before C++ appeared, in order to produce polymorphic behavior.
But doing so was complicated enough that C++ added compiler support to automate the process.
(Unfortunately, this gave me the impression that inheritance polymorphism should be used whenever possible).

This chapter generates a similar realization, but for functional programming.
Monads are so fundamental to functional programming that the Scala compiler provides direct support for this pattern in the form of the `for` comprehension (other functional languages have similar constructs).
Using monads in a `for` comprehension enables composition when the component functions are impure.

Although the `for` comprehension has multiple forms in Scala, we will primarily use the form shown in this chapter.

[^fn34]: From *What We Talk About When We Talk About Monads* by Thomas Petricek, [https://arxiv.org/ftp/arxiv/papers/1803/1803.10195.pdf](https://arxiv.org/ftp/arxiv/papers/1803/1803.10195.pdf).


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/monads/DesugaredComprehension.scala
```scala
package monads

val fc1 =
  for
    a <- Right("A")
    b <- Right("B")
    c <- Right("C")
  yield s"Result: $a $b $c"

val fc2 =
  Right("A").flatMap(a =>
    Right("B").flatMap(b =>
      Right("C").map(c => s"Result: $a $b $c")
    )
  )

@main
def expanded =
  println(fc1)
  println(fc2)

```


### experiments/src/main/scala/monads/Diapers.scala
```scala
package monads

import scala.util.Random

enum Diaper:

  def flatMap(f: String => Diaper): Diaper =
    this match
      case _: Empty =>
        this
      case s: Soiled =>
        f(
          s.description
        ) // written a different way for illustrating the different syntax options

  def map(f: String => String): Diaper =
    this match
      case _: Empty =>
        this
      case Soiled(description) =>
        Soiled(f(description))

  // optionally we can build this on top of
  // flatMap

  // flatMap(f.andThen(Soiled.apply))

  /* flatMap { description =>
   * Soiled(f(description)) } */

  case Empty()
  case Soiled(description: String)
end Diaper

def look: Diaper =
  val diaper =
    if (Random.nextBoolean())
      Diaper.Empty()
    else
      Diaper.Soiled("Ewwww")

  println(diaper)

  diaper

def change(description: String): Diaper =
  println("changing diaper")
  Diaper.Empty()

@main
def baby =
  val diaper: Diaper =
    for
      soiled <-
        look // When this returns Diaper.empty, we fall out and don't get to the left side
      freshy <-
        change(
          soiled
        ) // When this returns Diaper.empty, we fall out and don't get to the left side
    yield throw new RuntimeException(
      "This will never happen."
    ) // TODO Alter example so we don't have a pointless yield

  println(diaper)

```


### experiments/src/main/scala/monads/Flattening.scala
```scala
package monads

@main
def hmmm() =

  val abc = List("a", "bb", "ccc")
  println(abc.flatten)
  val abbccc =
    abc.flatMap { s =>
      s.toCharArray match
        case a: Array[Char] =>
          a.toList
        case null =>
          List.empty
    }
  println(abbccc)

  // We need to provide an instance of a function
  // that can
  // transform an Int to a IterableOnce[B]
  implicit def intToIterable(
      i: Int
  ): IterableOnce[Int] = List.fill(i)(i)

  val oneAndFiveFives = List(1, 5)
  println(oneAndFiveFives.flatten)
end hmmm

```


### experiments/src/main/scala/monads/FlowAndLaws.scala
```scala
package monads

enum Status:
  case Terminate
  case Continue

import Status.*

// Monads = Inversion of Flow Logic
case class Flow(
    status: Status,
    message: String = ""
):

  def flatMap(f: String => Flow): Flow =
    if (this == Flow.identity(message))
      f(message)
    else
      this

  def map(f: String => Status): Flow =
    flatMap(f.andThen(Flow(_)))

object Flow:

  def identity(message: String): Flow =
    Flow(Continue, message)

def doThing(s: String): Flow =
  println("doThing")
  Flow(Terminate, "terminating")

def doAnotherThing(s: String): Flow =
  println("doAnotherThing")
  Flow(Continue, "trying to unterminate")

@main
def imperative =
  val a = Flow(Continue, "starting")
  if (a.status == Continue)
    val b = doThing(a.message)
    if (b.status == Continue)
      val c = doAnotherThing(b.message)
      Terminate
    else
      b
  else
    a

@main
def monadic =
  println(
    for
      a <- Flow(Continue, "starting")
      b <- doThing(a)
      c <- doAnotherThing(b)
    yield Terminate
  )

// monads are a binary tree control flow
// structure
// the identity function is essential because it
// determines
// the path to take when performing an operation
// on the
// data held by the structure.

// Booleans suck. (they abstract away the
// meaning)

@main
def laws =
  // Left Identity Law
  assert(
    Flow.identity("asdf").flatMap(doThing) ==
      doThing("asdf")
  )

  assert(
    Flow
      .identity("asdf")
      .flatMap(_ =>
        Flow(Terminate, "something")
      ) == Flow(Terminate, "something")
  )

  // Right Identity Law
  // interesting question: if the monad calls the
  // flatMap functor with a value other than
  // what was passed in, does that violate the
  // monad right identity law
  assert(
    Flow
      .identity("original")
      .flatMap(Flow.identity) ==
      Flow.identity("original")
  )

  // starting value can be any monad instance
  assert(
    Flow(Terminate, "original")
      .flatMap(Flow.identity) ==
      Flow(Terminate, "original")
  )

  // Associativity Law
  def reverse(s: String): Flow =
    if (s.isEmpty)
      Flow(Terminate)
    else
      Flow(Continue, s.reverse)
  def upper(s: String): Flow =
    if (s.isEmpty)
      Flow(Terminate)
    else
      Flow(Continue, s.toUpperCase.toString)
  def reverseThenUpper(s: String): Flow =
    reverse(s).flatMap(upper)
  assert(
    Flow
      .identity("asdf")
      .flatMap(reverse)
      .flatMap(upper) ==
      Flow
        .identity("asdf")
        .flatMap(reverseThenUpper)
  )

  assert(
    Flow(Terminate, "asdf")
      .flatMap(reverse)
      .flatMap(upper) ==
      Flow(Terminate, "asdf")
        .flatMap(reverseThenUpper)
  )

  assert(
    Flow
      .identity("")
      .flatMap(reverse)
      .flatMap(upper) ==
      Flow.identity("").flatMap(reverseThenUpper)
  )
end laws

```


### experiments/src/main/scala/monads/GenericResult.scala
```scala
// Monads/GenericResult.scala
package monads

trait GResult[+W, +D]:
  def flatMap[W1, B](
      f: D => GResult[W1, B]
  ): GResult[W | W1, B] =
    println(s"flatMap() on $this")
    this.match
      case GSuccess(c) =>
        f(c)
      case fail: GFail[W] =>
        fail

  def map[B](f: D => B): GResult[W, B] =
    println(s"map() on $this")
    this.match
      case GSuccess(c) =>
        GSuccess(f(c))
      case fail: GFail[W] =>
        fail
end GResult

case class GFail[+W](why: W)
    extends GResult[W, Nothing]
case class GSuccess[+D](data: D)
    extends GResult[Nothing, D]

```


### experiments/src/main/scala/monads/Jackbot.scala
```scala
package monads
// Scratch/experimental example.

object Jackbot: // Keep from polluting the 'monads' namespace
  type Result[F, S]  = Either[F, S]
  type Fail[F, S]    = Left[F, S]
  type Succeed[F, S] = Right[F, S]
  object Fail:
    def apply[F](value: F) = Left.apply(value)
    def unapply[F, Any](fail: Fail[F, Any]) =
      Left.unapply(fail)
  object Succeed:
    def apply[S](value: S) = Right.apply(value)
    def unapply[Any, S](
        succeed: Succeed[Any, S]
    ) = Right.unapply(succeed)

@main
def jackbotWroteThis =
  val ok: Jackbot.Result[Int, String] =
    Jackbot.Succeed("Hello")
  val notOk: Jackbot.Result[Int, String] =
    Jackbot.Fail(4)
  val result1 =
    ok match
      case Jackbot.Succeed(value: String) =>
        value
      case Jackbot.Fail(code: Int) =>
        s"Failed with code ${code}"
      case _ =>
        s"Exhaustive fix"
  println(result1)
  val result2 =
    notOk match
      case Jackbot.Succeed(value: String) =>
        value
      case Jackbot.Fail(code: Int) =>
        s"Failed with code ${code}"
      case _ =>
        s"Exhaustive fix"
  println(result2)
end jackbotWroteThis

/* Output:
 * Hello Failed with code 4 */

```


### experiments/src/main/scala/monads/Operation.scala
```scala
package monads

enum Operation:

  def flatMap(
      f: String => Operation
  ): Operation =
    this match
      case _: Bad =>
        this
      case s: Good =>
        f(s.content)

  def map(f: String => String): Operation =
    this match
      case _: Bad =>
        this
      case Good(content) =>
        Good(f(content))

  case Bad(reason: String)
  case Good(content: String)
end Operation

def httpOperation(
    content: String,
    result: String
): Operation =
  if (content.contains("DbResult=Expected"))
    Operation
      .Good(content + s" + HttpResult=$result")
  else
    Operation
      .Bad("Did not have required data from DB")

def businessLogic(
    content: String,
    result: String
): Operation =
  if (content.contains("HttpResult=Expected"))
    Operation
      .Good(content + s" + LogicResult=$result")
  else
    Operation.Bad(
      "Did not have required data from Http Call"
    )

@main
def happyPath =
  println(
    for
      dbContent <-
        Operation.Good("DbResult=Expected")
      httpContent <-
        httpOperation(dbContent, "Expected")
      logicContent <-
        businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main
def sadPathDb =
  println(
    for
      dbContent <-
        Operation.Good("DbResult=Unexpected")
      httpContent <-
        httpOperation(dbContent, "Expected")
      logicContent <-
        businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main
def sadPathHttp =
  println(
    for
      dbContent <-
        Operation.Good("DbResult=Expected")
      httpContent <-
        httpOperation(dbContent, "Unexpected")
      logicContent <-
        businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main
def failAfterSpecifiedNumber =
  def operationConstructor(
      x: Int,
      limit: Int
  ): Operation =
    if (x < limit)
      Operation.Good(s"Finished step $x")
    else
      Operation.Bad("Failed after max number")

  def badAfterXInvocations(x: Int): Operation =
    for
      result1 <- operationConstructor(0, x)
      result2 <- operationConstructor(1, x)
      result3 <- operationConstructor(2, x)
      result4 <- operationConstructor(3, x)
    yield result4

  println(badAfterXInvocations(5))
end failAfterSpecifiedNumber

```


### experiments/src/main/scala/monads/Result.scala
```scala
// Monads/Result.scala
package monads

trait Result:
  def flatMap(f: String => Result): Result =
    println(s"flatMap() on $this")
    this.match
      case fail: Fail =>
        fail
      case Success(c) =>
        f(c)

  def map(f: String => String): Result =
    println(s"map() on $this")
    this.match
      case fail: Fail =>
        fail
      case Success(c) =>
        Success(f(c))

case class Fail(why: String)     extends Result
case class Success(data: String) extends Result

```


### experiments/src/main/scala/monads/ScratchResult.scala
```scala
package monads
// Scratch/experimental example.
// This doesn't work because the Either is
// contained in Result via composition.
// You can't inherit from Either because it's
// sealed.
// I think the solution is to make my own minimal
// Result only containing map and flatMap.

object ScratchResult: // Keep from polluting the 'monads' namespace
  class Result[F, S](val either: Either[F, S])

  case class Fail[F](fail: F)
      extends Result(Left(fail))

  case class Succeed[S](s: S)
      extends Result(Right(s))

@main
def essence = println("The essence of a monad")

```


### experiments/src/main/scala/monads/ShowGenericResult.scala
```scala
// Monads/ShowGenericResult.scala
// Exercise solution to "Verify
// GenericResult.scala works"
package monads

def gshow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, msg: String) =
    val result =
      if n == id then
        GFail(msg + id.toString)
      else
        GSuccess(msg + id.toString)
    println(s"op($id): $result")
    result

  val compose =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c

  if compose.isInstanceOf[GFail[String]] then
    println(s"Error-handling for $compose")
end gshow

@main
def gresults = 'a' to 'd' map gshow

```


### experiments/src/main/scala/monads/ShowResult.scala
```scala
// Monads/ShowResult.scala
package monads

def show(n: Char) =
  def op(id: Char, msg: String): Result =
    val result =
      if n == id then
        Fail(msg + id.toString)
      else
        Success(msg + id.toString)
    println(s"$n => op($id): $result")
    result

  val compose: Result =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Yielding: $c + 'd'")
      c + 'd'

  println(s"compose: $compose")
  compose match
    case Fail(why) =>
      println(s"Error-handling for $why")
    case Success(data) =>
      println("Successful case: " + data)

end show

@main
def results = 'a' to 'd' foreach show

```


### experiments/src/main/scala/monads/Solution1.scala
```scala
// Monads/Solution1.scala
package monads

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

  val compose =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c

  println(compose);
  for (failure <- compose.left)
    println(s"Error-handling for $failure")

end eshow

@main
def eresults = 'a' to 'd' foreach eshow

```


### experiments/src/main/scala/monads/Solution2.scala
```scala
// Monads/Solution2.scala
package monads

def ishow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, i: Int) =
    val result =
      if n == id then
        Left(i + id)
      else
        Right(i + id)
    println(s"op($id): $result")
    result

  val compose =
    for
      a: Int <- op('a', 0)
      b: Int <- op('b', a)
      c: Int <- op('c', b)
    yield
      println(s"Completed: $c")
      c

  println(compose);
  for (failure <- compose.left)
    println(s"Error-handling for $failure")

end ishow

@main
def iresults = 'a' to 'd' foreach ishow

```


### experiments/src/main/scala/monads/Solution3.scala
```scala
// Monads/Solution3.scala
package monads

def oshow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, msg: String) =
    val result =
      if n == id then
        None
      else
        Some(msg + id.toString)
    println(s"op($id): $result")
    result

  val compose =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c

  println(compose);
  if compose == None then
    println(s"Error-handling for None")

end oshow

@main
def oresults = 'a' to 'd' foreach oshow

```


### experiments/src/main/scala/monads/Solution4a.scala
```scala
// Monads/Solution4a.scala
package monads

enum ResultEnum:
  def flatMap(
      f: String => ResultEnum
  ): ResultEnum =
    println(s"flatMap() on $this")
    this.match
      case SuccessRE(c) =>
        f(c)
      case fail: FailRE =>
        fail

  def map(f: String => String): ResultEnum =
    println(s"map() on $this")
    this.match
      case SuccessRE(c) =>
        SuccessRE(f(c))
      case fail: FailRE =>
        fail

  case FailRE(why: String)
  case SuccessRE(data: String)

end ResultEnum

```


### experiments/src/main/scala/monads/Solution4b.scala
```scala
// Monads/Solution4b.scala
package monads

import ResultEnum.*

def showRE(n: Char) =
  def op(id: Char, msg: String): ResultEnum =
    val result =
      if n == id then
        FailRE(msg + id.toString)
      else
        SuccessRE(msg + id.toString)
    println(s"op($id): $result")
    result

  val compose: ResultEnum =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c.toUpperCase

  println(compose)
  compose match
    case FailRE(why) =>
      println(s"Error-handling for $why")
    case SuccessRE(data) =>
      println("Success: " + data)

end showRE

@main
def resultsRE = 'a' to 'd' foreach showRE

```


### experiments/src/main/scala/monads/Solution5.scala
```scala
package monads

val sol5a =
  for
    a <- Some("A")
    b <- Some("B")
    c <- Some("C")
  yield s"Result: $a $b $c"

val sol5b =
  Some("A").flatMap(a =>
    Some("B").flatMap(b =>
      Some("C").map(c => s"Result: $a $b $c")
    )
  )

@main
def sol5 =
  println(sol5a)
  println(sol5b)

```


### experiments/src/main/scala/monads/TypeConstructor.scala
```scala
package monads
// Just trying to understand what a type
// constructor is

// trait TypeConstructor[F[_], A]:
//  def use(tc: F): F[A]

trait Process[F[_], O]:
  def runLog(implicit
      F: MonadCatch[F]
  ): F[IndexedSeq[O]]

trait MonadCatch[F[_]]:
  def attempt[A](
      a: F[A]
  ): F[Either[Throwable, A]]
  def fail[A](t: Throwable): F[A]

```

            