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


### experiments/src/main/scala/monads/Operation.scala
```scala
package monads

// TODO Consider deletion, since we are reducing the scope of the book
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


