# Environment Exploration

NOTE: Much of this content was written before realizing that normal application-wide dependencies should not be passed in the environment.

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


## Use Cases

### Database Connections

Your application will manage an arbitrary number of database connections.
By making it part of the environment, you can start manipulating the runtime behavior in powerful ways:

- Batching requests
- ???

- Scope
- ???

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/19_Environment.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/environment/DatabaseConnection.scala
```scala
package environment

opaque type UserId = String
object UserId:
  def apply(str: String): UserId = str

case class DbConnection(
    actionLog: Ref[Seq[String]]
):
  def execute(label: String) =
    actionLog.update(_.appended(label))

object DbConnection:
  val make: ZLayer[Any, Nothing, DbConnection] =
    ZLayer.scoped(
      ZIO.acquireRelease(
        defer {
          val actionLog: Ref[Seq[String]] =
            Ref.make(Seq.empty[String]).run
          val connection =
            DbConnection(actionLog)
          connection.execute("OPEN").run
          connection
        }
      )(connection =>
        defer {
          connection.execute("CLOSE").run
          pprint.apply(connection)
        }.debug
      )
    )
end DbConnection

object DatabaseConnectionSimple
    extends ZIOAppDefault:

  def executeUserQueries(
      userId: UserId
  ): ZIO[DbConnection, Nothing, Unit] =
    // TODO Consider alternative version of this
    // where the defer happens inside of a
    // serviceWithZIO call
    defer {
      val connection =
        ZIO.service[DbConnection].run
      connection
        .execute(
          s"QUERY for $userId preferences"
            .stripMargin
        )
        .run
      ZIO.sleep(1.second).run
      connection
        .execute(
          s"QUERY for $userId accountHistory"
            .stripMargin
        )
        .run
    }

  // I prefer this version when there is only 1
  // Service needed from the environment
  // But if you need more, the first approach
  // needs to change less.
  def executeUserQueries2(
      userId: UserId
  ): ZIO[DbConnection, Nothing, Unit] =
    // TODO Consider alternative version of this
    // where the defer happens inside of a
    // serviceWithZIO call
    ZIO
      .serviceWithZIO[DbConnection](connection =>
        defer {
          connection
            .execute(
              s"QUERY for $userId preferences"
                .stripMargin
            )
            .run
          ZIO.sleep(1.second).run
          connection
            .execute(
              s"QUERY for $userId accountHistory"
                .stripMargin
            )
            .run
        }
      )

  def run =
    defer {
      executeUserQueries(UserId("Alice")).run
      executeUserQueries(UserId("Bob")).run
    }.provide(DbConnection.make)
end DatabaseConnectionSimple

object DatabaseConnectionInterleavedQueries
    extends ZIOAppDefault:

  def executeUserQueries(
      userId: UserId
  ): ZIO[DbConnection, Nothing, Unit] =
    // TODO Consider alternative version of this
    // where the defer happens inside of a
    // serviceWithZIO call
    defer {
      val connection =
        ZIO.service[DbConnection].run
      connection
        .execute(
          s"QUERY for $userId preferences"
            .stripMargin
        )
        .run
      ZIO.sleep(1.second).run
      connection
        .execute(
          s"QUERY for $userId accountHistory"
            .stripMargin
        )
        .run
    }

  // I prefer this version when there is only 1
  // Service needed from the environment
  // But if you need more, the first approach
  // needs to change less.
  def executeUserQueries2(
      userId: UserId
  ): ZIO[DbConnection, Nothing, Unit] =
    // TODO Consider alternative version of this
    // where the defer happens inside of a
    // serviceWithZIO call
    ZIO
      .serviceWithZIO[DbConnection](connection =>
        defer {
          connection
            .execute(
              s"QUERY for $userId preferences"
                .stripMargin
            )
            .run
          ZIO.sleep(1.second).run
          connection
            .execute(
              s"QUERY for $userId accountHistory"
                .stripMargin
            )
            .run
        }
      )

  def run =
    ZIO
      .foreachPar(
        List(UserId("Alice"), UserId("Bob"))
      )(executeUserQueries)
      .provide(DbConnection.make)
end DatabaseConnectionInterleavedQueries

```


### experiments/src/main/scala/environment/ToyEnvironment.scala
```scala
package environment

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

@annotation.nowarn
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

