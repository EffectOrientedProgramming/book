# Bruce's Miscellaneous Notes

Different types of polymorphism have different ways of expressing the commonality between the types that they work on. Inheritance polymorphism sets this commonality as all inherited from the same subtype. Parametric polymorphism allows the types to be disjoint and the commonality is expressed via the operations that are performed within the polymorphic function.

- If everyone follows some basic rules when creating their monad, we can automate a bunch of things for anything which is a monad. But what are those rules? In this case the rules are that you need a map and a flat map and these have to behave within certain constraints

Functional programming allows you to focus on the hard parts of the problem you're trying to solve, by automating everything else

Is a curly brace  enclosed block body the same as a lambda without arguments?

What if forward references are allowed within the context of a single chapter but they must be resolved by the end of that chapter? This still allows chapters to be easily moved around.


Abstraction delays implementation. This creates more choice.

A for comprehension creates a unit of work and the expressions within can be executed concurrently

Recursion is a tautology

Scala supports pure functional programming but it also allows other kinds of programming

Every program has an analysis phase and an interpretation phase. The interpreter decides if and when side effects occur, independent of the program definition. We choose The interpreter independently.

A value can hold a function and a function can hold a value so what's the real difference between Val and def? Val forces the evaluation of an expression, while def delays it

Code that runs quickly we do right now. If we don't know how long something will take or it takes too long we suspend it. Difference between blocking and non blocking.

Normal versus abnormal computations, everything is one of these and we either just execute it or do something special.

A four comprehension is kind of like an atomic operation

A type class is a default argument that can vary. It can change from configurations away from that function. Is it an inversion of control mechanism?

Transient versus permanent error

What are the benefits of error monads over exceptions?
With exceptions you always get a stack trace and its associated overhead. There may be additional context information that you need to save. With a monad you include as much or as little information as you need.

- Failing paths multiply when you compose them. Don't create failing paths in the first place and prevent any new failures from propagating.

- What is the problem that default arguments do not solve?

- Is the four comprehension the only mechanism in Scala that automatically unpacks a monad?

- ZIO creates reliability by solving the problems of failure and concurrency

- A system is statistically correct if it can be successful despite its failures. Statistical correctness supports rapid development languages over reliability oriented languages.

- Functional programming libraries do not narrow their usage arbitrarily. Each function is designed to be used in the broadest set of applications possible, without anticipating specific usage and thus prematurely constraining itself.

- The speed comes only after you've carefully crafted your set of primitives and then you can begin assembling your system without agonizing over failure at every step.

- The surprise of lack of thread safety in java. You can design a nice library without thinking about whether it is thread safe and then have to redesign it for thread safety.

- A case class with a single function argument becomes a reference to that function.

  ```scala
  case class f[A, B](g: A => B)

  h = f(_: Int => 10)

  h(1)
  ```

  Note that you can call `h` without referring to `g`, so `h` acts as a function reference to `g`.
