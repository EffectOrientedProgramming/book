scalaVersion := "3.0.2-RC2"

val zioVersion = "2.0.0-M3"

libraryDependencies ++=
  Seq(
    "dev.zio" %% "zio"          % zioVersion,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    "org.testcontainers"    % "testcontainers"            % "1.16.0",
    "org.testcontainers"    % "postgresql"            % "1.16.0",
    "org.testcontainers"    % "kafka"            % "1.16.0",
    "org.testcontainers"    % "mockserver"            % "1.16.0",
    "org.testcontainers"    % "toxiproxy"            % "1.16.0",
    "com.softwaremill.sttp.client3" %% "circe" %
      "3.3.15",
    "com.softwaremill.sttp.client3" %% "core" %
      "3.3.15",
    "io.getquill" %% "quill-jdbc" % "3.7.2.Beta1.4",
    "ch.qos.logback" % "logback-classic"                  % "1.2.3",
    // Or ZIO Modules
    "io.getquill" %% "quill-jdbc-zio" % "3.7.2.Beta1.4",
    // Postgres Async
    "io.getquill" %% "quill-jasync-postgres" % "3.7.2.Beta1.4",
  )
