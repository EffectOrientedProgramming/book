package monads

enum Status:
  case Terminate
  case Continue

import Status.*

// Monads = Inversion of Flow Logic
case class Flow(status: Status, message: String = ""):
  def flatMap(f: String => Flow): Flow =
    if (this == Flow.identity(message))
      f(message)
    else
      this

  def map(f: String => Status): Flow =
    flatMap(f.andThen(Flow(_)))

object Flow:
  def identity(message: String): Flow = Flow(Continue, message)

def doThing(s: String): Flow =
  println("doThing")
  Flow(Terminate, "terminating")

def doAnotherThing(s: String): Flow =
  println("doAnotherThing")
  Flow(Continue, "trying to unterminate")

@main def imperative =
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

@main def monadic =
  println(
    for
      a <- Flow(Continue, "starting")
      b <- doThing(a)
      c <- doAnotherThing(b)
    yield Terminate
  )

// monads are a binary tree control flow structure
// the identity function is essential because it determines
// the path to take when performing an operation on the
// data held by the structure.

// Booleans suck. (they abstract away the meaning)

@main def laws =
  // Left Identity Law
  assert(
    Flow.identity("asdf").flatMap(doThing) == doThing("asdf")
  )

  assert(
    Flow.identity("asdf").flatMap(_ => Flow(Terminate, "something")) == Flow(Terminate, "something")
  )

  // Right Identity Law
  // interesting question: if the monad calls the flatMap functor with a value other than
  // what was passed in, does that violate the monad right identity law
  assert(
    Flow.identity("original").flatMap(Flow.identity) == Flow.identity("original")
  )

  // starting value can be any monad instance
  assert(
    Flow(Terminate, "original").flatMap(Flow.identity) == Flow(Terminate, "original")
  )

  // Associativity Law
  def reverse(s: String): Flow = if (s.isEmpty) Flow(Terminate) else Flow(Continue, s.reverse)
  def upper(s: String): Flow = if (s.isEmpty) Flow(Terminate) else Flow(Continue, s.toUpperCase.toString)
  def reverseThenUpper(s: String): Flow = reverse(s).flatMap(upper)
  assert(
    Flow.identity("asdf").flatMap(reverse).flatMap(upper) == Flow.identity("asdf").flatMap(reverseThenUpper)
  )

  assert(
    Flow(Terminate, "asdf").flatMap(reverse).flatMap(upper) == Flow(Terminate, "asdf").flatMap(reverseThenUpper)
  )

  assert(
    Flow.identity("").flatMap(reverse).flatMap(upper) == Flow.identity("").flatMap(reverseThenUpper)
  )
