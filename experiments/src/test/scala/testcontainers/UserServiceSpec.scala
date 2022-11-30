package testcontainers

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.github.scottweaver.models.JdbcInfo
import zio.test.*
import zio.*
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.live
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.Settings
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import org.postgresql.ds.PGSimpleDataSource
import zio.test.TestAspect.ignore

import java.sql.Connection
import javax.sql.DataSource

object UserServiceSpec
/* extends ZIOSpec[DataSource & JdbcInfo]:
 * val bootstrap = SharedDbLayer.layer def spec =
 * suite("UserService")( test("retrieves an
 * existin user")( for user <- UserService
 * .get("uuid_hard_coded") .debug yield
 * assertCompletes ), test("inserts a user") {
 * val newUser =
 * User("user_id_from_app", "Appy") for _ <-
 * UserService.insert(newUser) user <-
 * UserService.get(newUser.userId) yield
 * assertTrue(newUser == user) }
 * ).provideSomeShared[DataSource](
 * UserServiceLive.layer ) end UserServiceSpec */
