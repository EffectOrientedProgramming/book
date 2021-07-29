val zioVersion = "2.0.0-M1"

enablePlugins(MdocPlugin)

name := "EffectOrientedProgramming"

scalaVersion := "3.0.2-RC1"

scalacOptions += "-Yexplicit-nulls"
scalacOptions -= "-explain-types"
scalacOptions -= "-explain"
scalacOptions -= "-encoding"

libraryDependencies ++=
  Seq(
    "org.jetbrains" % "annotations-java5" %
      "15.0",
    "org.scalameta" %
      "scalafmt-dynamic" % "3.0.0-RC6" cross
      CrossVersion.for3Use2_13,
    "dev.zio" %% "zio" % zioVersion,
    "com.typesafe" % "config" % "1.4.1",
    //     cross CrossVersion.for3Use2_13,
    "dev.zio" %% "zio-test" % zioVersion % Test,
    "dev.zio" %% "zio-test-sbt" % zioVersion %
      Test,
    "org.scalameta" %% "munit" % "0.7.27" % Test
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
