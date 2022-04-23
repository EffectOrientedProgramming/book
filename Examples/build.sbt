scalaVersion := "3.1.1"

val zioVersion = "2.0.0-RC4"

libraryDependencies ++=
  Seq(
    "dev.zio" %% "zio"          % zioVersion,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    "org.testcontainers"    % "testcontainers"            % "1.16.2",
    "org.testcontainers"    % "postgresql"            % "1.16.2",
    "org.testcontainers"    % "kafka"            % "1.16.2",
    "org.testcontainers"    % "mockserver"            % "1.16.2",
    "org.testcontainers"    % "toxiproxy"            % "1.16.2",
    "com.softwaremill.sttp.client3" %% "circe" %
      "3.3.16",
    "com.softwaremill.sttp.client3" %% "core" %
      "3.3.16",
    "io.getquill" %% "quill-jdbc" % "3.7.2.Beta1.4",
    "ch.qos.logback" % "logback-classic"                  % "1.2.11",
    // Or ZIO Modules
    "io.getquill" %% "quill-jdbc-zio" % "3.7.2.Beta1.4",
    // Postgres Async
    "io.getquill" %% "quill-jasync-postgres" % "3.7.2.Beta1.4",
    "io.github.arkinator" % "mockserver-client-java" % "5.11.7",
    "org.apache.kafka" % "kafka-clients" % "3.1.0",
    "org.postgresql" % "postgresql" % "42.3.1"
  )
