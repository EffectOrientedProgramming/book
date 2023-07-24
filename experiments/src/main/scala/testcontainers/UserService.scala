package testcontainers

import io.getquill.*

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
  import ctx.{lift, run, *}

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
