scalaVersion := "3.0.2-RC1"

val zioVersion = "2.0.0-M1"

libraryDependencies ++=
  Seq(
    "dev.zio" %% "zio"          % zioVersion,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion %
      Test
  )
