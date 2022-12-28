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


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/environment_exploration/ToyEnvironment.scala
```scala
package environment_exploration

import scala.reflect.{ClassTag, classTag}

case class DBService(url: String)

// Yada yada yada lets talk about the environment
trait ToyEnvironmentT[+R]:

  def add[A: ClassTag](
      a: A
  ): ToyEnvironmentT[R & A]

  def get[A >: R: ClassTag]: A

class ToyEnvironment[+R](
    typeMap: Map[ClassTag[_], Any]
) extends ToyEnvironmentT[R]:

  def add[A: ClassTag](
      a: A
  ): ToyEnvironment[R & A] =
    ToyEnvironment(typeMap + (classTag[A] -> a))

  def get[A >: R: ClassTag]: A =
    typeMap(classTag[A]).asInstanceOf[A]

@main
def demoToyEnvironment =
  val env: ToyEnvironment[_] =
    ToyEnvironment(Map.empty)

  val env1: ToyEnvironment[String] =
    env.add("hi")

  val env2: ToyEnvironment[String & DBService] =
    env1.add(DBService("blah"))

  val env3: ToyEnvironment[
    String & DBService & List[String]
  ] = env2.add(List("a", "b"))

  println(env3.get[String])
  println(env3.get[DBService])
  println(env3.get[List[String]])

  // We get some amount of compile time safety
  // here, but not much
  // println(env.get(classOf[List[DBService]]))

  // Downside of the current approach is that it
  // doesn't prevent duplicate types
  env3.add("hi") // is accepted
end demoToyEnvironment

// Consider this runtime de-duping

class ToyEnvironmentRuntimeDeduplication[+R](
    typeMap: Map[ClassTag[_], Any]
):

  def add[A: ClassTag](
      a: A
  ): ToyEnvironment[R & A] =
    if (typeMap.contains(classTag[A]))
      throw new IllegalArgumentException(
        s"Cannot add ${classTag[A]} to environment, it already exists"
      )
    else
      ToyEnvironment(
        typeMap + (classTag[A] -> a)
      )

```


### experiments/src/main/scala/environment_exploration/TupledEnvironmentZio.scala
```scala
package environment_exploration

// trait TypeTag // TODO Or ClassTag?
// trait TypeInstance

// case class TypeMap(
//   typeMap: Map[TypeTag, TypeInstance]
// )

case class TupledEnvironmentZio[ENV, RESULT](
    run: ENV => RESULT
):
  def unsafeRun(env: ENV): RESULT = run(env)

  // The tuple here is a step towards the
  // full-featured TypeMap that ZIO uses
  def flatMap[ENV2, RESULT2](
      f: RESULT => TupledEnvironmentZio[
        ENV2,
        RESULT2
      ]
  ): TupledEnvironmentZio[(ENV, ENV2), RESULT2] =
    TupledEnvironmentZio((env, env2) =>
      f(run(env)).run(env2)
    )

@main
def demoSingleEnvironmentInstance =
  val customTypeMapZio
      : TupledEnvironmentZio[Int, String] =
    TupledEnvironmentZio(env =>
      val result = env * 10
      s"result: $result"
    )
  println(customTypeMapZio.unsafeRun(5))

  val repeatMessage
      : TupledEnvironmentZio[Int, String] =
    TupledEnvironmentZio(env =>
      s"Message \n" * env
    )
  println(repeatMessage.unsafeRun(5))

case class BigResult(message: String)
@main
def demoTupledEnvironment =
  val squared: TupledEnvironmentZio[Int, Unit] =
    TupledEnvironmentZio(env =>
      println(
        "Environment integer squared: " +
          env * env
      )
    )

  val repeatMessage
      : TupledEnvironmentZio[String, BigResult] =
    TupledEnvironmentZio(message =>
      BigResult(s"Environment message: $message")
    )

  val composedRes: TupledEnvironmentZio[
    (Int, String),
    BigResult
  ] = squared.flatMap(_ => repeatMessage)

  val finalResult =
    composedRes.unsafeRun((5, "Hello"))
  println(finalResult)
end demoTupledEnvironment

import zio.ZIO

```

            