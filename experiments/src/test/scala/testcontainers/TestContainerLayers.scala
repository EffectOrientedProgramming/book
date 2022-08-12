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
