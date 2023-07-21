package testcontainers

import io.github.scottweaver.models.JdbcInfo
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import zio.test.TestAspect.{before, beforeAll}

object DbMigration:

  type ConfigurationCallback =
    (FluentConfiguration) => FluentConfiguration

  private def doMigrate(
      jdbcInfo: JdbcInfo,
      configureCallback: ConfigurationCallback,
      locations: String*
  ) =
    ZIO.attempt {
      val flyway =
        configureCallback {
          val flyway =
            Flyway
              .configure()
              .dataSource(
                jdbcInfo.jdbcUrl,
                jdbcInfo.username,
                jdbcInfo.password
              )

          if (locations.nonEmpty)
            flyway.locations(locations: _*)
          else
            flyway
        }.load()
      flyway.migrate
    }

  def migrate(mirgationLocations: String*)(
      configureCallback: ConfigurationCallback =
        identity
  ) =
    ZIO
      .service[JdbcInfo]
      .flatMap(jdbcInfo =>
        doMigrate(
          jdbcInfo,
          configureCallback,
          mirgationLocations: _*
        )
      )
      .orDie

  def migratedLayer(
      jdbcInfo: ZEnvironment[JdbcInfo]
  ): ZLayer[Any, Throwable, Unit] =
    ZLayer.fromZIO(
      DbMigration
        .migrate("db")()
        .provideEnvironment(jdbcInfo)
        .unit
    )
end DbMigration
