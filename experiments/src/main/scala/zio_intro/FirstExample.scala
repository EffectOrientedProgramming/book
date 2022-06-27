package zio_intro

import zio.{Clock, ZIO, ZIOAppDefault, System}
import zio.Console.{readLine, printLine}

object FirstExample extends ZIOAppDefault:
  def run =
    for
      _    <- printLine("Give us your name:")
      name <- readLine
      _    <- printLine(s"$name")
    yield ()

object HelloWorld extends ZIOAppDefault:
  def run = printLine("Hello World")

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
    for
      users       <- activeUsers
      currentUser <- user
      authenticatedUser <-
        authenticateUser(users, currentUser)
    yield authenticatedUser

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
