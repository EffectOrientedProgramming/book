package experiments

import zio.*
import zio.Console.*
import zio.config.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.*

case class MyConfig(ldap: String, port: Int, dburl: String)

val configDescriptor = deriveConfig[MyConfig]

val configProvider =
  ConfigProvider.fromHoconString:
    "{ ldap: Foo babble, port: 42, dburl: Bar wiggly }"

val configuration =
  ZLayer.fromZIO:
    read:
      configDescriptor.from:
        configProvider

object Configuration extends ZIOAppDefault:
  def run =
    ZIO
      .serviceWithZIO[MyConfig]:
        myConfig =>
          val ldap = myConfig.ldap
          val port = myConfig.port
          val dburl = myConfig.dburl
          printLine(s"ldap = $ldap, port = $port, dburl = $dburl")
      .provide:
        configuration
