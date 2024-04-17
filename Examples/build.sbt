ThisBuild / scalaVersion := "3.4.1"

val zioVersion = "2.0.22"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-direct" % "1.0.0-RC7",
  "dev.zio" %% "zio-config" % "4.0.1",
  "dev.zio" %% "zio-config-magnolia" % "4.0.1",
  "dev.zio" %% "zio-config-typesafe" % "4.0.1",
  "dev.zio" %% "zio-cache" % "0.2.3",
  "nl.vroste" %% "rezilience" % "0.9.4",
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
)

fork := true
