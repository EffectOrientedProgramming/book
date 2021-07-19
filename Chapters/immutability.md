# Immutable

Part of the Functional Programming world is the lack of mutable variables. 
Mutable variables, being defined variables that can change their values after instantiation,
pose a problem to pure functions. Part of the definition pure functions is that they are non-reliant 
on the state of external variables. Mutable variables provide a level of uncertainty in how a 
function may behave. For example, given a function `f`:

>f(y) = x + y

The output of `f` is dependent on the value of x. If x changes between calls of the function, the output
my vary even if the inputs remain the same.

> x = 2
> f(y = 3) = (2) + (3) = 5
> x = 4
> f(y = 3) = (4) + (3) = 7

Even though the input remained the same, the output of the function changed. This violates one of the 
rules of a pure function. 

In Scala terms, this means using `Val`s instead of `Var`s, and `For Comprehensions` instead of `For Loops`.






Vals everywhere.
Inputs and outputs.
Functions and expressions, avoid statements.


