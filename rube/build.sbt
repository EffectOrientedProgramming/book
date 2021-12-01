libraryDependencies ++= Seq(
  "io.getquill" %% "quill-jdbc" % "3.7.2.Beta1.4",
  "ch.qos.logback" % "logback-classic"                  % "1.2.7",
  // Or ZIO Modules
  "io.getquill" %% "quill-jdbc-zio" % "3.7.2.Beta1.4",
  // Postgres Async
  "io.getquill" %% "quill-jasync-postgres" % "3.7.2.Beta1.4",
  "io.circe"  % "circe-core_3"  % "0.15.0-M1",
  "io.circe" %% "circe-generic" % "0.15.0-M1",
  "com.softwaremill.sttp.client3" %% "circe" %
    "3.3.18",
  "com.softwaremill.sttp.client3" %% "core" %
    "3.3.18",

  "org.apache.kafka" % "kafka-clients" % "3.0.0",

  "org.testcontainers"    % "testcontainers"            % "1.16.1",
  "org.testcontainers"    % "postgresql"            % "1.16.1",
  "org.testcontainers"    % "kafka"            % "1.16.1",
  "org.testcontainers"    % "mockserver"            % "1.16.1",
  "org.testcontainers"    % "toxiproxy"            % "1.16.1",
  "io.github.arkinator" % "mockserver-client-java" % "5.11.9",
  "org.postgresql" % "postgresql" % "42.3.0"
)

configs(IntegrationTest)

Defaults.itSettings