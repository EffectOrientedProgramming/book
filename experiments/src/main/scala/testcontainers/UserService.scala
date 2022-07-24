package testcontainers

import javax.sql.DataSource


trait UserNotFound
trait User

trait UserService {
  def get(userId: String): ZIO[Nothing, UserNotFound, User]
}

case class UserServiceLive(dataSource: DataSource) {
//  import QuillContext._
  def get(userId: String): ZIO[Nothing, UserNotFound, User]
}
