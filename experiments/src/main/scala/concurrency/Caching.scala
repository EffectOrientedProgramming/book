package concurrency

import zio.{ZIO, ZIOAppDefault, ZLayer}
import zio.cache.{Cache, Lookup}
import zio_helpers.repeatNPar

import java.nio.file.Path

val thunderingHerdsScenario =
  defer:
    val popularService =
      ZIO.service[PopularService].run

    ZIO
      .repeatNPar(100):
        popularService.retrieve:
          Path.of("awesomeMemes")
      .run

    val cloudStorage =
      ZIO.service[CloudStorage].run

    cloudStorage.invoice.debug.run

object PopularService:
  private def lookup(key: Path) =
    defer:
      val cloudStorage =
        ZIO.service[CloudStorage].run

      cloudStorage.retrieve(key).run

  val make =
    defer:
      val cloudStorage =
        ZIO.service[CloudStorage].run
      PopularService(cloudStorage.retrieve)

  val makeCached =
    defer:
      val cache =
        Cache
          .make(
            capacity = 100,
            timeToLive = Duration.Infinity,
            lookup = Lookup(lookup)
          )
          .run

      PopularService(cache.get)
end PopularService

object NoCache extends ZIOAppDefault:
  override def run =
    thunderingHerdsScenario.provide(
      CloudStorage.live,
      ZLayer.fromZIO(PopularService.make)
    )

object WithCache extends ZIOAppDefault:
  override def run =
    thunderingHerdsScenario.provide(
      CloudStorage.live,
      ZLayer.fromZIO(PopularService.makeCached)
    )

// invisible to bottom

// TODO Figure if these functions belong in the object instead.
case class FSLive(requests: Ref[Int])
    extends CloudStorage:
  def retrieve(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    defer:
      requests.update(_ + 1).run
      ZIO.sleep(10.millis).run
      FSLive.hardcodedContents

  val invoice: ZIO[Any, Nothing, String] =
    defer:
      val count = requests.get.run

      "Amount owed: $" + count

object FSLive:
  val hardcodedContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )
end FSLive

case class FileContents(contents: List[String])

trait CloudStorage:
  def retrieve(
      name: Path
  ): ZIO[Any, Nothing, FileContents]
  val invoice: ZIO[Any, Nothing, String]

object CloudStorage:
  val live =
    ZLayer.fromZIO:
      defer:
        FSLive(Ref.make(0).run)

case class PopularService(
    retrieveContents: Path => ZIO[
      Any,
      Nothing,
      FileContents
    ]
):
  def retrieve(name: Path) =
    retrieveContents(name)
