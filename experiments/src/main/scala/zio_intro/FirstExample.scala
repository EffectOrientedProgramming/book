package zio_intro

import zio.{Clock, Has, ZIO, ZIOAppDefault, System, ZServiceBuilder}
import zio.Console.{readLine, printLine}

object FirstExample extends ZIOAppDefault:
  def run =
    for
      _ <- printLine("Give us your name:")
      name <- readLine
      _ <- printLine(s"$name")
    yield ()
    
object HelloWorld extends ZIOAppDefault:
  def run = printLine("Hello World")
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
object AuthenticationFlow extends ZIOAppDefault:
  val activeUsers: ZIO[Has[Clock], DiskError, List[UserName]] = ???
  
  val user: ZIO[Has[System], Nothing, UserName] = ???
  
  def authenticateUser(users: List[UserName], currentUser: UserName): ZIO[Any, UnauthenticatedUser, AuthenticatedUser] = ???
      

  val fullAuthenticationProcess: ZIO[Has[Clock] & Has[System], DiskError | UnauthenticatedUser, AuthenticatedUser] =
    for
      users <- activeUsers
      currentUser <- user
      authenticatedUser <- authenticateUser(users, currentUser)
    yield  authenticatedUser
      
  
  def run =
    fullAuthenticationProcess
      .provideServices(zio.ZEnv.live)
      .orDieWith(error => new Exception("Unhandled error: " + error))


















trait UserName
case class FileSystem()
trait DiskError
trait EnvironmentVariableNotFound
case class UnauthenticatedUser(msg: String)
case class AuthenticatedUser(userName: UserName)
