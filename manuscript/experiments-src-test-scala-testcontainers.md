## experiments-src-test-scala-testcontainers

 

### experiments/src/test/scala/testcontainers/DbMigration.scala
```scala
package testcontainers

import io.github.scottweaver.models.JdbcInfo
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import zio.*
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

```


### experiments/src/test/scala/testcontainers/SharedDbLayer.scala
```scala
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

```


### experiments/src/test/scala/testcontainers/TestContainerLayers.scala
```scala
package testcontainers

import com.zaxxer.hikari.{
  HikariConfig,
  HikariDataSource
}
import io.github.scottweaver.models.JdbcInfo
import zio.*

import java.util.Properties
import javax.sql.DataSource
import scala.jdk.CollectionConverters.MapHasAsJava

object TestContainerLayers:

  val dataSourceLayer
      : ZLayer[JdbcInfo, Nothing, DataSource] =
    ZLayer {
      for
        jdbcInfo <- ZIO.service[JdbcInfo]
        datasource <-
          ZIO
            .attemptBlocking(
              unsafeDataSourceFromJdbcInfo(
                jdbcInfo
              )
            )
            .orDie
      yield datasource
    }

  private def unsafeDataSourceFromJdbcInfo(
      jdbcInfo: JdbcInfo
  ): DataSource =
    val props = new Properties()
    props.putAll(
      Map(
        "driverClassName" ->
          jdbcInfo.driverClassName,
        "jdbcUrl"  -> jdbcInfo.jdbcUrl,
        "username" -> jdbcInfo.username,
        "password" -> jdbcInfo.password
      ).asJava
    )
    println("JdbcInfo: " + jdbcInfo)
    new HikariDataSource(new HikariConfig(props))
end TestContainerLayers

```


### experiments/src/test/scala/testcontainers/UserActionSpec.scala
```scala
package testcontainers

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.github.scottweaver.models.JdbcInfo
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer.{
  Settings,
  live
}
import org.postgresql.ds.PGSimpleDataSource
import zio.*
import zio.test.*

import java.sql.Connection
import javax.sql.DataSource

object UserActionSpec
/* extends ZIOSpec[DataSource & JdbcInfo]:
 * val bootstrap = SharedDbLayer.layer
 *
 * def spec =
 * suite("UserActionService")( test("inserts a
 * user") { for _ <- UserActionService
 * .get("uuid_hard_coded") .debug("Actions")
 * yield assertCompletes }
 * ).provideSomeShared[DataSource](
 * UserActionServiceLive.layer ) */

```


### experiments/src/test/scala/testcontainers/UserServiceSpec.scala
```scala
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

```

