package testcontainers

import zio.*
import io.getquill._
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig

object DummyQuill

object QuillLocal:
  type AppPostgresContext =
    PostgresJdbcContext[
      io.getquill.LowerCase.type
    ]
  def configFromContainer(
      container: PostgresContainerJ
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
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)
    config

  val quillPostgresContext: ZLayer[Has[
    PostgresContainerJ
  ], Nothing, Has[AppPostgresContext]] =
    ZLayer
      .service[PostgresContainerJ]
      .map(_.get)
      .flatMap {
        (safePostgres: PostgresContainerJ) =>

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
      run(somePeople)

end QuillLocal
