val quillVersion = "3.19.0"

libraryDependencies ++= Seq(
  "io.getquill" %% "quill-jdbc" % quillVersion,
  "ch.qos.logback" % "logback-classic"                  % "1.2.11",
  // Or ZIO Modules
  "io.getquill" %% "quill-jdbc-zio" % quillVersion,
  // Postgres Async
  "io.getquill" %% "quill-jasync-postgres" % quillVersion,
  "io.circe"  % "circe-core_3"  % "0.15.0-M1",
  "io.circe" %% "circe-generic" % "0.15.0-M1",
  "com.softwaremill.sttp.client3" %% "circe" %
    "3.7.1",
  "com.softwaremill.sttp.client3" %% "core" %
    "3.7.1",

  "org.apache.kafka" % "kafka-clients" % "3.2.0",

  "org.testcontainers"    % "testcontainers"            % "1.17.3",
  "org.testcontainers"    % "postgresql"            % "1.17.3",
  "org.testcontainers"    % "kafka"            % "1.17.3",
  "org.testcontainers"    % "mockserver"            % "1.17.3",
  "org.testcontainers"    % "toxiproxy"            % "1.17.3",
  "io.github.arkinator" % "mockserver-client-java" % "5.11.9",
  "org.postgresql" % "postgresql" % "42.4.0"
)

configs(IntegrationTest)

Defaults.itSettings