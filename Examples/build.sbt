ThisBuild / scalaVersion := "3.4.1"

val zioVersion = "2.0.22"

// add only these imports by default
scalacOptions +=
  Seq(
    "java.lang",
    "scala",
    "scala.Predef",
    "zio",
    "zio.direct",
  ).mkString(
    start = "-Yimports:",
    sep = ",",
    end = ""
  )

Test / scalacOptions += "-Yimports:zio.test"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-direct" % "1.0.0-RC7",
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
)

fork := true
