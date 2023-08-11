ThisBuild / scalaVersion := "3.3.0"

val zioVersion = "2.0.15"

lazy val mdoctools = (project in file("mdoctools"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
  ),
  scalacOptions +=
    Seq(
      "java.lang",
      "scala",
      "scala.Predef",
      "zio",
    ).mkString(
      start = "-Yimports:",
      sep = ",",
      end = ""
    )
)

lazy val root = (project in file("."))
  .dependsOn(mdoctools)

scalacOptions +=
  Seq(
    "java.lang",
    "scala",
    "scala.Predef",
    "zio",
    "zio.direct",
    "mdoctools",
  ).mkString(
    start = "-Yimports:",
    sep = ",",
    end = ""
  )

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-cache" % "0.2.3",
  "dev.zio" %% "zio-concurrent" % zioVersion,
  "dev.zio" %%
    "zio-direct" % "1.0.0-RC7" excludeAll
    (
      "com.geirsson",
      "metaconfig-typesafe-config"
    ) excludeAll
    (
      "com.geirsson",
      "metaconfig-core"
    ) excludeAll
    ("org.typelevel", "paiges-core"),
  "dev.zio" %% "zio-logging" % "2.1.14",
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-test" % zioVersion,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "dev.zio" %% "zio-prelude" % "1.0.0-RC19",

  "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % "0.9.0",
  "io.github.scottweaver" %% "zio-2-0-db-migration-aspect" % "0.9.0",
  "io.getquill" %% "quill-jdbc" % "3.7.2.Beta1.4",
  "io.getquill" %% "quill-jdbc-zio" % "4.6.0.1",
  "io.getquill" %% "quill-zio" % "4.6.0.1",
)