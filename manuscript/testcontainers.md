## testcontainers

 

### experiments/src/main/scala/testcontainers/InteractWithDatabase.scala
```scala
package testcontainers

import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import zio.*

object InteractWithDatabase
    extends ZIOAppDefault:
//  val logic =
//    for {
//      _ <-
//
//    }

  def run =
    UserService
      .get("blah")
      .provide(
        UserServiceLive.layer,
//      UserActionServiceLive.layer,
        QuillContext.dataSourceLayer
      )

```


### experiments/src/main/scala/testcontainers/QuillContext.scala
```scala
package testcontainers

import com.typesafe.config.ConfigFactory
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.jdbczio.Quill
import io.getquill.{
  NamingStrategy,
  PluralizedTableNames,
  PostgresZioJdbcContext,
  SnakeCase
}
import zio.*

import javax.sql.DataSource
import scala.jdk.CollectionConverters.MapHasAsJava

/** QuillContext houses the datasource layer
  * which initializes a connection pool. This has
  * been slightly complicated by the way Heroku
  * exposes its connection details. Database URL
  * will only be defined when run from Heroku in
  * production.
  */
object QuillContext
    extends PostgresZioJdbcContext(
      NamingStrategy(
        PluralizedTableNames,
        SnakeCase
      )
    ):
  val dataSourceLayer
      : ZLayer[Any, Nothing, DataSource] =
    ZLayer {
      for
        _ <- ZIO.debug("Hi")
        herokuURL <-
          System.env("DATABASE_URL").orDie
        _ <- ZIO.debug("Bye")
        localDBConfig =
          Map(
            "dataSource.user"     -> "postgres",
            "dataSource.password" -> "",
            "dataSource.url" ->
              "jdbc:postgresql://localhost:5432/postgres"
          )
        configMap =
          herokuURL
            .map(parseHerokuDatabaseUrl(_).toMap)
            .getOrElse(localDBConfig)
        config =
          ConfigFactory.parseMap(
            configMap
              .updated(
                "dataSourceClassName",
                "org.postgresql.ds.PGSimpleDataSource"
              )
              .asJava
          )
      yield Quill
        .DataSource
        .fromConfig(config)
        .orDie
    }.flatten

  /** HerokuConnectionInfo is a wrapper for the
    * datasource information to make it
    * compatible with Heroku
    */
  final case class HerokuConnectionInfo(
      username: String,
      password: String,
      host: String,
      port: String,
      dbname: String
  ):
    def toMap: Map[String, String] =
      Map(
        "dataSource.user"     -> username,
        "dataSource.password" -> password,
        "dataSource.url" ->
          s"jdbc:postgresql://$host:$port/$dbname"
      )

  /** Parses the necessary information out of the
    * Heroku formatted URL
    */
  def parseHerokuDatabaseUrl(
      string: String
  ): HerokuConnectionInfo =
    string match
      case s"postgres://$username:$password@$host:$port/$dbname" =>
        HerokuConnectionInfo(
          username,
          password,
          host,
          port,
          dbname
        )
end QuillContext

```


### experiments/src/main/scala/testcontainers/UserActionService.scala
```scala
package testcontainers

import io.getquill.{Query, Quoted}
import zio.*

import java.sql.SQLException
import java.time.{Instant, LocalDateTime}
import javax.sql.DataSource

enum ActionType:
  case LogIn,
    LogOut,
    UpdatePreferences

case class UserAction(
    userId: String,
    actionType: ActionType,
    timestamp: LocalDateTime
)

trait UserActionService:
  def get(
      userId: String
  ): ZIO[Any, UserNotFound, List[UserAction]]
  def insert(
      user: UserAction
  ): ZIO[Any, Nothing, Long]

object UserActionService:
  def get(
      userId: String
  ): ZIO[UserActionService, UserNotFound, List[
    UserAction
  ]] =
    ZIO.serviceWithZIO[UserActionService](x =>
      x.get(userId)
    ) // use .option ?

  def insert(
      user: UserAction
  ): ZIO[UserActionService, Nothing, Long] =
    ZIO.serviceWithZIO[UserActionService](x =>
      x.insert(user)
    )

final case class UserActionServiceLive(
    dataSource: DataSource
) extends UserActionService:
  import io.getquill._
  // SnakeCase turns firstName -> first_name
  val ctx =
    new PostgresZioJdbcContext(
      NamingStrategy(
        PluralizedTableNames,
        SnakeCase
      )
    )
  import ctx._

  inline def runWithSourceQuery[T](
      inline quoted: Quoted[Query[T]]
  ): ZIO[Any, SQLException, List[T]] =
    run(quoted).provideEnvironment(
      ZEnvironment(dataSource)
    )

  inline def runWithSourceInsert[T](
      inline quoted: Quoted[Insert[T]]
  ): ZIO[Any, SQLException, Long] =
    run(quoted).provideEnvironment(
      ZEnvironment(dataSource)
    )

  import java.util.UUID

  implicit val encodeUserAction
      : MappedEncoding[ActionType, String] =
    MappedEncoding[ActionType, String](
      _.toString
    )
  implicit val decodeUserAction
      : MappedEncoding[String, ActionType] =
    MappedEncoding[String, ActionType](
      ActionType.valueOf(_)
    )

  implicit val encodeUUID
      : MappedEncoding[Instant, String] =
    MappedEncoding[Instant, String](_.toString)
  implicit val decodeUUID
      : MappedEncoding[String, Instant] =
    MappedEncoding[String, Instant](
      Instant.parse(_)
    )

  def get(
      userId: String
  ): ZIO[Any, UserNotFound, List[UserAction]] =
    inline def somePeople =
      quote {
        query[UserAction]
          .filter(_.userId == lift(userId))
      }
    runWithSourceQuery(somePeople).orDie

  def insert(
      user: UserAction
  ): ZIO[Any, Nothing, Long] =
    inline def insert =
      quote {
        query[UserAction].insertValue(lift(user))
      }
    runWithSourceInsert(insert).orDie
end UserActionServiceLive

object UserActionServiceLive:
  val layer
      : URLayer[DataSource, UserActionService] =
    ZLayer.fromFunction(
      UserActionServiceLive.apply _
    )

```


### experiments/src/main/scala/testcontainers/UserService.scala
```scala
package testcontainers

import io.getquill.{Query, Quoted}
import zio.*
import io.getquill._

import java.sql.SQLException
import javax.sql.DataSource

trait UserNotFound
case class User(userId: String, name: String)

trait UserService:
  def get(
      userId: String
  ): ZIO[Any, UserNotFound, User]
  def insert(user: User): ZIO[Any, Nothing, Long]
  // TODO update(user)

object UserService:
  def get(userId: String): ZIO[
    UserService with DataSource,
    UserNotFound,
    User
  ] =
    ZIO.serviceWithZIO[UserService](
      _.get(userId)
    ) // use .option ?

  def insert(user: User): ZIO[
    UserService with DataSource,
    Nothing,
    Long
  ] = // TODO Um? Why Nothing?????
    ZIO.serviceWithZIO[UserService](
      _.insert(user)
    )

final case class UserServiceLive(
    dataSource: DataSource
) extends UserService:
  // SnakeCase turns firstName -> first_name

  val ctx =
    new PostgresZioJdbcContext(
      NamingStrategy(
        PluralizedTableNames,
        SnakeCase
      )
    )
  import ctx.{run, lift, _}

  def get(
      userId: String
  ): ZIO[Any, UserNotFound, User] =
    inline def somePeople =
      quote {
        query[User]
          .filter(_.userId == lift(userId))
      }
    run(somePeople)
      .provideEnvironment(
        ZEnvironment(dataSource)
      )
      .orDie
      .map(_.head)

  def insert(
      user: User
  ): ZIO[Any, Nothing, Long] =
    inline def insert =
      quote {
        query[User].insertValue(lift(user))
      }
    run(insert)
      .provideEnvironment(
        ZEnvironment(dataSource)
      )
      .orDie
end UserServiceLive

object UserServiceLive:
  val layer =
    ZLayer.fromZIO(
      for datasource <- ZIO.service[DataSource]
      yield UserServiceLive(datasource)
    )

```


