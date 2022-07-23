package testcontainers
import io.github.scottweaver.models.JdbcInfo
import zio.test.*
import zio.*
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.live
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.Settings
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import io.github.scottweaver.zio.aspect.DbMigrationAspect

import javax.sql.DataSource

object ContainerSpec extends ZIOSpecDefault {
  def spec =
    (suite("hi")(
    test("there")(
      for {
        _ <- ZIO.service[DataSource]
      } yield assertCompletes
    )
  ).provide(live, Settings.default,
//    JdbcInfo.apply()
//    ZPostgreSQLContainer.Settings.default,
//    ZPostgreSQLContainer.live,
//    TestContainerLayers.dataSourceLayer,
  ) @@ DbMigrationAspect.migrateOnce("db")()).provide(

      ZPostgreSQLContainer.Settings.default,
      ZPostgreSQLContainer.live
//      ZLayer.succeed(
//        JdbcInfo(
//          driverClassName = "driver class",
//          jdbcUrl = "jdbc_url",
//          username =  "username",
//          password = "password"
//        )
//      )
    )
}
