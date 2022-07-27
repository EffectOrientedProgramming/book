package testcontainers

import io.getquill.{Query, Quoted}
import zio.*

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource


trait UserNotFound
case class UserAction(userId: String, actionType: String, timestamp: Instant)

trait UserActionService {
  def get(userId: String): ZIO[Any, UserNotFound, List[UserAction]]
  def insert(user: UserAction): ZIO[Any, Nothing, Long]
}

object UserActionService:
  def get(userId: String): ZIO[UserActionService with DataSource, UserNotFound, List[UserAction]] =
    ZIO.serviceWithZIO[UserActionService](x => x.get(userId)) // use .option ?

  def insert(user: UserAction): ZIO[UserActionService with DataSource, Nothing, Long] = // TODO Um? Why Nothing?????
    ZIO.serviceWithZIO[UserActionService](x => x.insert(user))

final case class UserActionServiceLive(dataSource: DataSource) extends UserActionService {
  import io.getquill._
  // SnakeCase turns firstName -> first_name
  val ctx = new PostgresZioJdbcContext(SnakeCase)
  import ctx._

  inline def runWithSourceQuery[T](inline quoted: Quoted[Query[T]]): ZIO[Any, SQLException, List[T]] =
    run(quoted).provideEnvironment(ZEnvironment(dataSource))

  inline def runWithSourceInsert[T](inline quoted: Quoted[Insert[T]]): ZIO[Any, SQLException, Long] =
    run(quoted).provideEnvironment(ZEnvironment(dataSource))

  import java.util.UUID

  implicit val encodeUUID: MappedEncoding[Instant, String] = MappedEncoding[Instant, String](_.toString)
  implicit val decodeUUID: MappedEncoding[String, Instant] = MappedEncoding[String, Instant](Instant.parse(_))

  def get(userId: String): ZIO[Any, UserNotFound, List[UserAction]] =
    inline def somePeople = quote {
      query[UserAction].filter(_.userId == lift(userId))
    }
    runWithSourceQuery(somePeople).orDie

  def insert(user: UserAction): ZIO[Any, Nothing, Long] =
    inline def insert = quote {
      query[UserAction].insertValue(lift(user))
    }
    runWithSourceInsert(insert).orDie

}

object UserActionServiceLive:
  val layer: URLayer[DataSource, UserActionService] =
    ZLayer.fromFunction(UserActionServiceLive.apply _)