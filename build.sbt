enablePlugins(MdocPlugin)

name := "EffectOrientedProgramming"

scalaVersion := "3.0.2-RC1"

scalacOptions += "-Yexplicit-nulls"
scalacOptions -= "-explain-types"
scalacOptions -= "-explain"
scalacOptions -= "-encoding"

val zioVersion = "2.0.0-M1"

libraryDependencies ++=
  Seq(
    "org.jetbrains" % "annotations-java5" %
      "22.0.0",
    "org.scalameta"      %
      "scalafmt-dynamic" % "3.0.0-RC7" cross
      CrossVersion.for3Use2_13,
    "dev.zio"     %% "zio"    % zioVersion,
    "com.typesafe" % "config" % "1.4.1",
    //     cross CrossVersion.for3Use2_13,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion %
      Test,
    "org.scalameta" %% "munit" % "0.7.28" % Test,
    "io.circe"  % "circe-core_3"  % "0.15.0-M1",
    "io.circe" %% "circe-generic" % "0.15.0-M1",
    "com.softwaremill.sttp.client3" %% "circe" %
      "3.3.13",
    "com.softwaremill.sttp.client3" %% "core" %
      "3.3.13"
// "io.d11" %% "zhttp" % "1.0.0.0-RC17", //
    // TODO Check for updates supporting ZIO2
    // milestones
// "io.d11" %% "zhttp-test" % "1.0.0.0-RC17"
    // % Test,
//    "dev.zio" %% "zio-json" % "0.2.0-M1"
  )

testFrameworks +=
  new TestFramework(
    "zio.test.sbt.ZTestFramework"
  )

testFrameworks +=
  new TestFramework("munit.Framework")

mdocIn := file("Chapters")

mdocOut := file("manuscript")

scalafmtOnCompile := true
