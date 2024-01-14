package async

import zio.*
import zio.direct.*
import scala.concurrent.Future

object AsyncOrSyncDontMatter
    extends ZIOAppDefault:

  def timesTwoSyncFuture(i: Int): Future[Int] =
    Future.successful(i * 2)

  // aync version that just returns the int
  def timesTwoAsyncFuture(i: Int): Future[Int] =
    ???

  /* def timesFour(i: Int): Int =
   * val j = timesTwoSync(1) timesTwoAsync(j) */

  def timesTwoAsync(i: Int) =
    defer:
      ZIO.sleep(1.second).run
      i * 2

  def timesTwoSync(i: Int) =
    defer:
      i * 2

  def timesFour(i: Int) =
    defer:
      val j = timesTwoAsync(i).run
      timesTwoSync(j).run

  override def run =
    defer:
      timesTwoAsync(1).debug.run
      timesTwoSync(1).debug.run
      timesFour(1).debug.run

  import scala.concurrent.Await
  // If you don't await, then Future percolates
  // up the call stack, and forces many other
  // data types to also be coerced into Futures
  def shouldThrottleUserBlocking(userId: Int) =
    val privateMessageCount =
      getPrivateMessages(userId)
    val future = getPostsByUserFuture(userId)
    val postCount =
      Await.result(
        future,
        scala
          .concurrent
          .duration
          .Duration("1 second")
      )
    privateMessageCount + postCount > 10

  def shouldThrottleUser(userId: Int) =
    defer:
      val privateMessageCount =
        getPrivateMessages(userId)
      val postCount =
        getPostsByUserZio(userId).run
      privateMessageCount + postCount > 10

  def getPrivateMessages(userId: Int) = userId

  def getPostsByUserFuture(
      userId: Int
  ): Future[Int] =
    Thread.sleep(1000)
    Future.successful(userId)

  def getPostsByUserZio(
      userId: Int
  ): ZIO[Any, Nothing, Int] =
    defer:
      ZIO.sleep(1.second).run
      userId
end AsyncOrSyncDontMatter
