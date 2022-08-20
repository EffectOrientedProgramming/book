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

object UserActionSpec extends ZIOSpec[DataSource & JdbcInfo] {
  val bootstrap =
    SharedDbLayer.layer
    
  def spec =
    (suite("UserActionService")(
      test("inserts a user"){
        for {
          _ <- UserActionService.get("uuid_hard_coded").debug("Actions")
        } yield assertCompletes
      },
    ) ).provideSomeShared[DataSource](
      UserActionServiceLive.layer,
    )
}
