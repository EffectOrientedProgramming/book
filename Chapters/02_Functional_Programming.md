# Functional Programming

""" {{ copied from a blog post }}
a programming paradigm where programs are constructed by applying and composing functions. It is a declarative programming paradigm based on a sequence of functions that only depend on each other in terms of arguments and return values.

Functional programming is good at handling complexity using function composition, splitting difficult problems into manageable problems.
Then the functions that solve these manageable problems are composed together to solve the original difficult problem.
Functions obtained with this process tend to be small, increasing their reusability, maintainability, and readability.

Although the initial development time can increase with such restrictions, the increased maintainability compensates for the effort.

Compiler Driven Development. In short: change parts of the code and then let the compiler errors guide you in the rest of the task.
"""

## Pure Functions
The most familiar example of a pure function is a mathematical function.

```text
f(a, b) = a + b
```
Here
- `f` is the function name
- Inside the parentheses, `a` and `b` are the *parameters*, which hold the input values
- After the `=`, `a+b` is the expression/implementation that produces the result

In Scala, we would express this as:

```scala
def f(a: Int, b: Int): Int = a + b
```

- `def` is the keyword for defining a function
- `f` is the function name
- The parameters `a` and `b` each have *type annotations*, which tell the compiler what kind of parameter to expect
  * The type `Int` indicates an Integer value
- The function itself *also* has a *type annotation* indicating that it returns an `Int`
- `=` assigns the expression `a + b` to the function `f`

This function is *pure* because the parameters completely determine its behavior, and it has no impact on the world.

## Pure Functional Programming
### Referential transparency




