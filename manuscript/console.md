# Console

## The Unprincipled Way

This is generally the first effect that we will want as we learn to construct functional programs.
It is so basic that most languages do not consider it as anything special.
The typical first scala program is something like:

```scala
println("Hi there.")
// Hi there.
```

Simple enough, and familiar to anyone that has programmed before.
Take a look at the signature of this function in the Scala `Predef` object:

```scala
def println(x: Any): Unit = ???
```

Based on the name, it is likely that the `Console` is involved.
Unfortunately the type signature does not indicate that.
If we check the implementation, we discover this:

```scala
def println(x: Any): Unit = Console.println(x)
```

Now it is clear that we are printing to the `Console`.
If we do not have access to the implementation source code, this is a surprise to us at runtime.


## The ZIO Way