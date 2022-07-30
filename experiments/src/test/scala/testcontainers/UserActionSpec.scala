package testcontainers

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.github.scottweaver.models.JdbcInfo
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.{Settings, live}
import org.postgresql.ds.PGSimpleDataSource
import zio.*
import zio.test.*

import java.sql.Connection
import javax.sql.DataSource

object SharedDbLayer:
  val layer =
    for {
      layer <- ZLayer.make[DataSource & JdbcInfo](
        ZPostgreSQLContainer.live,
        ZPostgreSQLContainer.Settings.default,
      )
      _ <- ZLayer.fromZIO(DbMigration.migrate("db")().provideEnvironment(layer))
    } yield layer

object UserActionSpec extends ZIOSpec[DataSource & JdbcInfo] {
  val bootstrap =
    SharedDbLayer.layer
  val testContainerSource: ZLayer[JdbcInfo, Nothing, DataSource] = TestContainerLayers.dataSourceLayer
  val postgres: ZLayer[Settings, Nothing, JdbcInfo & Connection &
    PGSimpleDataSource
      & PostgreSQLContainer] = ZPostgreSQLContainer.live
  def spec =
    (suite("UserActionService")(
      test("inserts a user"){
        val newUser = User("user_id_from_app", "Appy")
        for {
          _ <- UserActionService.get("uuid_hard_coded").debug("Actions")
        } yield assertCompletes
      },
    ) ).provideSomeShared[DataSource](
      UserActionServiceLive.layer,
    )
}
