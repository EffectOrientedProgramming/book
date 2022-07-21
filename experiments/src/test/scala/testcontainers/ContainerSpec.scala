package testcontainers
import io.github.scottweaver.models.JdbcInfo
import zio.test.*
import zio.*
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.live
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.Settings

object ContainerSpec extends ZIOSpecDefault {
  def spec = suite("hi")(
    test("there")(
      for {
        _ <- ZIO.service[JdbcInfo]
      } yield assertCompletes
    )
  ).provide(live, Settings.default)
}
