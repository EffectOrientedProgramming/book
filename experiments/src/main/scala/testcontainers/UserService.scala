package testcontainers

import io.getquill.{Query, Quoted}
import zio.*

import java.sql.SQLException
import javax.sql.DataSource


case class AppUser(userId: String, name: String)

trait UserService {
  def get(userId: String): ZIO[Any, UserNotFound, AppUser]
  def insert(user: AppUser): ZIO[Any, Nothing, Long]
}

object UserService:
  def get(userId: String): ZIO[UserService with DataSource, UserNotFound, AppUser] =
    ZIO.serviceWithZIO[UserService](x => x.get(userId)) // use .option ?

  def insert(user: AppUser): ZIO[UserService with DataSource, Nothing, Long] = // TODO Um? Why Nothing?????
    ZIO.serviceWithZIO[UserService](x => x.insert(user))

final case class UserServiceLive(dataSource: DataSource) extends UserService {
  import io.getquill._
  // SnakeCase turns firstName -> first_name
  val ctx = new PostgresZioJdbcContext(SnakeCase)
  import ctx._

  inline def runWithSourceQuery[T](inline quoted: Quoted[Query[T]]): ZIO[Any, SQLException, List[T]] =
    run(quoted).provideEnvironment(ZEnvironment(dataSource))

  inline def runWithSourceInsert[T](inline quoted: Quoted[Insert[T]]): ZIO[Any, SQLException, Long] =
    run(quoted).provideEnvironment(ZEnvironment(dataSource))

  def get(userId: String): ZIO[Any, UserNotFound, AppUser] =
    inline def somePeople = quote {
      query[AppUser].filter(_.userId == lift(userId))
    }
    runWithSourceQuery(somePeople).orDie.map(_.head)

  def insert(user: AppUser): ZIO[Any, Nothing, Long] =
    inline def insert = quote {
      query[AppUser].insertValue(lift(user))
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