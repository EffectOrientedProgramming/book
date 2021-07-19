# Functional Programming

> [[[The goal of this chapter is to go from zero to the "Schrödinger's Cheshire Cat" program.
> This will set the reader up for the "What is an Effect" Chapter.]]]

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




