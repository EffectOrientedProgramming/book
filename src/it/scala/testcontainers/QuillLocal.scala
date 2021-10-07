package testcontainers

import zio.*
import zio.Console.*
import java.io.IOException
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import io.getquill._

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

      val age = 18
      inline def somePeople =
        quote {
          query[Person]
            .filter(p => p.age > lift(age))
        }
      val people: List[Person] = run(somePeople)
      // TODO Get SQL
      people

end QuillLocal
