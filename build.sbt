import scala.concurrent.duration._

/*
  Some options:
    1. Hub Examples
    2. Crypto Examples
    3. GitHub Action improvements
      - Recognize when `runAll` is happening over on Github, and use a fake console
    4. Something totally different
 */

val zioVersion = "1.0.9"

val testPar: sbt.TaskKey[scala.Unit] = TaskKey("runAll")

lazy val root = (project in file("."))
  .settings(
    MdToSource / mdDirectory := file("Chapters"),
    name := "effectOrientedProgramming",
    version := "0.0.1",
    scalaVersion := "3.0.1",
    resolvers += Resolver.JCenterRepository,
    libraryDependencies ++= Seq(
      "org.jetbrains" % "annotations-java5" % "15.0",
      "dev.zio" %% "zio" % zioVersion,
      "org.scalameta" % "mdoc_3" % "2.2.21",
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "org.scalameta" %% "munit" % "0.7.26" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    testFrameworks += new TestFramework("munit.Framework"),
    testPar := Def.taskDyn {
      val allMainTasks = (Compile / discoveredMainClasses).value.map { m =>
        (Compile / runMain).toTask(s" $m")
      }
      Def.sequential(allMainTasks)
    }.value,
    scalacOptions += "-Yexplicit-nulls",
    scalacOptions -= "-explain-types",
    scalacOptions -= "-explain"
  )
  .enablePlugins(MdToSourcePlugin)

lazy val docs = project // new documentation project
  .settings(
    MdToSource / mdDirectory := file("Chapters"),
    scalaVersion := "3.0.1",
    mdocIn := file("Chapters"),
    mdocOut := file("manuscript")
  )
  .in(file("myproject-docs")) // important: it must not be docs/
  .dependsOn(root)
  .enablePlugins(MdocPlugin)
