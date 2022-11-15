scalaVersion := "3.2.1"

val zioVersion = "2.0.3"

libraryDependencies ++=
  Seq(
    "dev.zio" %% "zio"          % zioVersion,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % "0.9.0",
    "io.github.scottweaver" %% "zio-2-0-db-migration-aspect" % "0.9.0",
//    "com.softwaremill.sttp.client3" %% "circe" %
//      "3.3.16",
//    "com.softwaremill.sttp.client3" %% "core" %
//      "3.3.16",
    "io.getquill" %% "quill-jdbc" % "3.7.2.Beta1.4",
//    "ch.qos.logback" % "logback-classic"                  % "1.2.11",
    // Or ZIO Modules
    "io.getquill" %% "quill-jdbc-zio" % "4.6.0",
    "io.getquill" %% "quill-zio" % "4.6.0",
    // Postgres Async
//    "io.getquill" %% "quill-jasync-postgres" % "3.7.2.Beta1.4",
//    "io.github.arkinator" % "mockserver-client-java" % "5.11.7",
//    "org.apache.kafka" % "kafka-clients" % "3.2.0",
//    "org.postgresql" % "postgresql" % "42.3.1"
  )
