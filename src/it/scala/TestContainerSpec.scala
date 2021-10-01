package mdoc

import zio.*
import zio.Console.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*

import java.io.IOException

import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network
}

import io.getquill._
import mdoc.QuillLocal.AppPostgresContext

case class Person(
    firstName: String,
    lastName: String,
    age: Int
)

object QuillLocal:
  type AppPostgresContext =
    PostgresJdbcContext[
      io.getquill.LowerCase.type
    ]
  def configFromContainer(
      container: PostgresContainer
  ) =
    val pgDataSource =
      new org.postgresql.ds.PGSimpleDataSource()

    val exposedPort =
      container.getMappedPort(5432).nn
    val username = container.getUsername.nn
    val password = container.getPassword.nn
    pgDataSource.setUser(username)
    pgDataSource
      .setPortNumbers(Array(exposedPort))
    pgDataSource.setPassword(password)
    import com.zaxxer.hikari.HikariConfig
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)
    config

  val quillPostgresContext: ZLayer[Has[
    PostgresContainer
  ], Nothing, Has[AppPostgresContext]] =
    ZLayer
      .service[PostgresContainer]
      .map(_.get)
      .flatMap {
        (safePostgres: PostgresContainer) =>
          import com.zaxxer.hikari.HikariDataSource

          val config =
            configFromContainer(safePostgres)
          ZLayer.succeed(
            new PostgresJdbcContext(
              LowerCase,
              new HikariDataSource(config)
            )
          )
      }

  val quillQuery: ZIO[Has[
    AppPostgresContext
  ], Nothing, List[Person]] =
    for
      ctx <- ZIO.service[AppPostgresContext]
    yield
      import ctx._

      val named = "Joe"
      inline def somePeople =
        quote {
          query[Person].filter(p =>
            p.firstName == lift(named)
          )
        }
      val people: List[Person] = run(somePeople)
      // TODO Get SQL
      people
end QuillLocal

object ManagedTestInstances:
  lazy val networkLayer
      : ZLayer[Any, Nothing, Has[Network]] =
    ZManaged
      .acquireReleaseWith(
        ZIO.debug("Creating network") *>
          ZIO.succeed(Network.newNetwork().nn)
      )((n: Network) =>
        ZIO.attempt(n.close()).orDie *>
          ZIO.debug("Closing network")
      )
      .toLayer

// TODO Figure out fi
// TESTCONTAINERS_RYUK_DISABLED=true is a
// band-aid that's avoiding the real problem with
// test cleanup

object TestContainersSpec
    extends DefaultRunnableSpec:

  import zio.durationInt

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test("With managed layer") {
        // TODO
        val logic =
          for
            people <- QuillLocal.quillQuery
            _ <- ZIO.debug(".").repeatN(1000)
          yield assert(people)(
            equalTo(
              List(Person("Joe", "Dimagio", 143))
            )
          )

        logic.provideSomeLayer[ZTestEnv & ZEnv](
          ManagedTestInstances.networkLayer >>>
            (PostgresContainer
              .construct("init.sql") >>>
            QuillLocal.quillPostgresContext) ++

              KafkaContainerLocal
                .construct()
        )
      }
    )
end TestContainersSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
