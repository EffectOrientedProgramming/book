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
If there's a problem you'll need to handle it right after the function is called, which is awkward and often produces duplicate code.
People probably won't use our system unless we figure out a way to simplify and automate box manipulation.

What if we had a standard set of operations that work on all boxes, to automate the use of our system and eliminate all that duplicated code?
The box---and the associated operations---is a monad.

## The Error Monad

To introduce monads, we will start with the problem of handling errors.
This is the most compelling initial use for monads.

Encountering an error during a function call generally means two things:

1. You can't continue executing the function in the normal fashion.
2. You can't return a normal result.

Many languages use *exceptions* for handling errors.
An exception *throws* out of the current execution path to locate a user-written *handler* to deal with the error.
There are two goals for exceptions:

1. Separate error-handling code from "success-path" code, so the success-path code is easier to understand and reason about.

2. Reduce redundant error-handling code by handling errors in a single place.

TODO: Need better insights on the problems with exceptions
The problem with exceptions is that they lose important context information when they are thrown.
Also it separates "normal failure" from "exceptional failure" (Map get() example).

- Exercise: show that GenericResult.scala works with ShowResult.scala
- Exercise: Modify Above solution to work with `Int` instead of `String`
- Exercise: Show that `Either` works with ShowResult.scala
- Exercise: modify ShowResult.scala to work with `Option`
- Exercise: Modify GenericResult.scala to create GenericOption.scala, implementing your on version of `Option`
