package zio_intro

object AuthenticationFlow extends ZIOAppDefault:
  val activeUsers
      : ZIO[Any, DiskError, List[UserName]] = ???

  val user: ZIO[Any, Nothing, UserName] = ???

  def authenticateUser(
      users: List[UserName],
      currentUser: UserName
  ): ZIO[
    Any,
    UnauthenticatedUser,
    AuthenticatedUser
  ] = ???

  val fullAuthenticationProcess: ZIO[
    Any,
    DiskError | UnauthenticatedUser,
    AuthenticatedUser
  ] =
    defer {
      val users       = activeUsers.run
      val currentUser = user.run
      authenticateUser(users, currentUser).run
    }

  def run =
    fullAuthenticationProcess.orDieWith(error =>
      new Exception("Unhandled error: " + error)
    )
end AuthenticationFlow

trait UserName
case class FileSystem()
trait DiskError
trait EnvironmentVariableNotFound
case class UnauthenticatedUser(msg: String)
case class AuthenticatedUser(userName: UserName)
