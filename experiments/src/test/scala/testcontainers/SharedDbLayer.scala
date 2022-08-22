package testcontainers

import io.github.scottweaver.models.JdbcInfo
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import zio.ZLayer

import javax.sql.DataSource

object SharedDbLayer:
  val layer =
    for
      layer <-
        ZLayer.make[DataSource & JdbcInfo](
          ZPostgreSQLContainer.live,
          ZPostgreSQLContainer.Settings.default
        )
      _ <- DbMigration.migratedLayer(layer)
    yield layer
