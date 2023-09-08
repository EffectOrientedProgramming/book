## environment

 

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


