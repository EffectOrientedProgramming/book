package testcontainers

import zio.*
import javax.sql.DataSource


trait UserNotFound
case class AppUser(userId: String, name: String)

trait UserService {
  def get(userId: String): ZIO[DataSource, UserNotFound, AppUser]
}

object UserService:
  def get(userId: String): ZIO[UserService with DataSource, UserNotFound, AppUser] = // TODO Um? Why Nothing?????
    ZIO.serviceWithZIO[UserService](x => x.get(userId))

final case class UserServiceLive(dataSource: DataSource) extends UserService {
  import io.getquill._
  // SnakeCase turns firstName -> first_name
  println("A")
  val ctx = new PostgresZioJdbcContext(SnakeCase)
  println("B")
  import ctx._

  def get(userId: String): ZIO[DataSource, UserNotFound, AppUser] =
    inline def somePeople = quote {
      query[AppUser].filter(_.userId == lift(userId))
    }
    run(somePeople).provideEnvironment(ZEnvironment(dataSource)).orDie.map(_.head)
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