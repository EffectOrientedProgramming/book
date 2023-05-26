## monads

 

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


