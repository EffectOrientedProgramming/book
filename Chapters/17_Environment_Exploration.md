# Environment Exploration

The Environment type parameter distinguishes `ZIO` from most other IO monads.
At first, it might seem like a complicated way of passing values to your ZIO instances - why can't they just be normal function arguments?

- The developer does not need to manually plug an instance of type `T` into every `ZIO[T, _, _]` in order to run them.
- `ZIO` instances can be freely composed, flatmapped, etc before ever providing the required environment. It's only needed at the very end when we want to execute the code!
- Environments can be arbitrarily complex, without requiring a super-container type to hold all of the fields.
- Compile time guarantees that you have 
  1. Provided everything required
  1. Have not provided multiple, conflicting instances of the same type

## Dependency Injection 
*TODO Decide if this requires a tangent, or just a single mention to let people know we're solving the same type of problem. TODO*

## ZEnvironment: Powered by a TypeMap
ZIO is able to accomplish all this through the `ZEnvironment` class. 

*TODO Figure out how/where to include the disclaimer that we're stripping out many of the implementation details TODO*

```scala
ZEnvironment[+R](map: Map[LightTypeTag, (Any, Int)])
```

The crucial data structure inside is a `Map[LightTypeTag, (Any)]`.
*TODO Decide how much to dig into `LightTypeTag` vs `Tag[A]` TODO*
Seeing `Any` here might be confusing - `ZEnvironment` is supposed to give us type-safety when executing `ZIO`s!
Looking at the `get` method, we see specic, typed results.

```scala
def get[A >: R](implicit tagged: Tag[A]): A
```

How is this possible when all of our `Map` values are `Any`s?
`add` holds the answer.

```scala 
def add[A](a: A): ZEnvironment[R with A]
```

Even though each new entry is stored as an `Any`, we store knowledge of our new entry in the `R` type parameter.
We append the type of our new `A` to the `R` type parameter, and get back a brand-new environment that we know contains all types from the original *and* the type of the instance we just added.

Now look at the `get` implementation to see how this is used.

```scala
  def get[A](tag: Tag[A]): A =
    val lightTypeTag = taggedTagType(tag)

    self.map.get(lightTypeTag) match:
      case Some(a) => a.asInstanceOf[A]
      case None => throw new Error(s"Defect: ${tag} not inside ${self}")

  private def taggedTagType[A](tagged: Tag[A]): LightTypeTag
```

## ZLayer
- Better Ergonomics than ZEnvironment
- Shared by default
