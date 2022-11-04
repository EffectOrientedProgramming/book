package testcontainers

import com.typesafe.config.ConfigFactory
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.jdbczio.Quill
import io.getquill.{
  NamingStrategy,
  PluralizedTableNames,
  PostgresZioJdbcContext,
  SnakeCase
}
import zio.*

import javax.sql.DataSource
import scala.jdk.CollectionConverters.MapHasAsJava

/** QuillContext houses the datasource layer
  * which initializes a connection pool. This has
  * been slightly complicated by the way Heroku
  * exposes its connection details. Database URL
  * will only be defined when run from Heroku in
  * production.
  */
object QuillContext
    extends PostgresZioJdbcContext(
      NamingStrategy(
        PluralizedTableNames,
        SnakeCase
      )
    ):
  val dataSourceLayer
      : ZLayer[Any, Nothing, DataSource] =
    ZLayer {
      for
        _ <- ZIO.debug("Hi")
        herokuURL <-
          System.env("DATABASE_URL").orDie
        _ <- ZIO.debug("Bye")
        localDBConfig =
          Map(
            "dataSource.user"     -> "postgres",
            "dataSource.password" -> "",
            "dataSource.url" ->
              "jdbc:postgresql://localhost:5432/postgres"
          )
        configMap =
          herokuURL
            .map(parseHerokuDatabaseUrl(_).toMap)
            .getOrElse(localDBConfig)
        config =
          ConfigFactory.parseMap(
            configMap
              .updated(
                "dataSourceClassName",
                "org.postgresql.ds.PGSimpleDataSource"
              )
              .asJava
          )
      yield Quill
        .DataSource
        .fromConfig(config)
        .orDie
    }.flatten

  /** HerokuConnectionInfo is a wrapper for the
    * datasource information to make it
    * compatible with Heroku
    */
  final case class HerokuConnectionInfo(
      username: String,
      password: String,
      host: String,
      port: String,
      dbname: String
  ):
    def toMap: Map[String, String] =
      Map(
        "dataSource.user"     -> username,
        "dataSource.password" -> password,
        "dataSource.url" ->
          s"jdbc:postgresql://$host:$port/$dbname"
      )

  /** Parses the necessary information out of the
    * Heroku formatted URL
    */
  def parseHerokuDatabaseUrl(
      string: String
  ): HerokuConnectionInfo =
    string match
      case s"postgres://$username:$password@$host:$port/$dbname" =>
        HerokuConnectionInfo(
          username,
          password,
          host,
          port,
          dbname
        )
end QuillContext
