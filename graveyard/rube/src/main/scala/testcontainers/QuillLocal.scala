package testcontainers

import io.getquill._
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import zio.ZLayer

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
      container.getMappedPort(5432)
    val username =
      container.getUsername
    val password =
      container.getPassword
    pgDataSource.setUser(username)
    pgDataSource
      .setPortNumbers(Array(exposedPort))
    pgDataSource.setPassword(password)
    val config =
      new HikariConfig()
    config.setDataSource(pgDataSource)
    config
  end configFromContainer

  val quillPostgresContext: ZLayer[
    PostgresContainerJ,
    Nothing,
    AppPostgresContext
  ] =
    ZLayer
      .service[PostgresContainerJ]
      .flatMap {
        (
          safePostgresE: ZEnvironment[
            PostgresContainerJ
          ]
        ) =>
          val safePostgres =
            safePostgresE.get

          val config =
            configFromContainer(safePostgres)
          ZLayer.succeed(
            new PostgresJdbcContext(
              LowerCase,
              new HikariDataSource(config)
            )
          )
      }

  // todo: Could not unift-serialize the 'class
  // io.getquill.ast.Filter'
  // Filter(Entity("Person", List()), Id("p"),
  // BinaryOperation(Property(Id("p"), "age"), >,
  // ScalarTag("d817ee45-7a77-4acf-be78-e1484ba45e84"))).
  // Performing a regular unlift instead.
  val quillQuery
      : ZIO[AppPostgresContext, Nothing, List[
        Person
      ]] =
    ???
/* val quillQuery : ZIO[AppPostgresContext,
 * Nothing, List[ Person ]] =
 * for ctx <- ZIO.service[AppPostgresContext]
 * yield import ctx._
 *
 * val age = 18 inline def somePeople =
 * quote { query[Person] .filter(p => p.age >
 * lift(age)) } run(somePeople) */

end QuillLocal
