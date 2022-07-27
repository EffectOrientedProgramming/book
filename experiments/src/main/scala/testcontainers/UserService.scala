package testcontainers

import io.getquill.{Query, Quoted}
import zio.*

import java.sql.SQLException
import javax.sql.DataSource

trait UserNotFound
case class User(userId: String, name: String)

trait UserService {
  def get(userId: String): ZIO[Any, UserNotFound, User]
  def insert(user: User): ZIO[Any, Nothing, Long]
}

object UserService:
  def get(userId: String): ZIO[UserService with DataSource, UserNotFound, User] =
    ZIO.serviceWithZIO[UserService](_.get(userId)) // use .option ?

  def insert(user: User): ZIO[UserService with DataSource, Nothing, Long] = // TODO Um? Why Nothing?????
    ZIO.serviceWithZIO[UserService](_.insert(user))

final case class UserServiceLive(dataSource: DataSource) extends UserService {
  import io.getquill._
  // SnakeCase turns firstName -> first_name

  val ctx = new PostgresZioJdbcContext(NamingStrategy(PluralizedTableNames, SnakeCase))
  import ctx._

  inline def runWithSourceQuery[T](inline quoted: Quoted[Query[T]]): ZIO[Any, SQLException, List[T]] =
    run(quoted).provideEnvironment(ZEnvironment(dataSource))

  inline def runWithSourceInsert[T](inline quoted: Quoted[Insert[T]]): ZIO[Any, SQLException, Long] =
    run(quoted).provideEnvironment(ZEnvironment(dataSource))

  def get(userId: String): ZIO[Any, UserNotFound, User] =
    inline def somePeople = quote {
      query[User].filter(_.userId == lift(userId))
    }
    runWithSourceQuery(somePeople).orDie.map(_.head)

  def insert(user: User): ZIO[Any, Nothing, Long] =
    inline def insert = quote {
      query[User].insertValue(lift(user))
    }
    runWithSourceInsert(insert).orDie

}

object UserServiceLive:
  val layer: URLayer[DataSource, UserService] =
    ZLayer.fromFunction(UserServiceLive.apply _)
//    ZLayer.fromZIO(
//    for {
//      ds <- ZIO.service[DataSource]
//    } yield UserServiceLive(ds)
//    )
//    ZLayer.fromFunction()