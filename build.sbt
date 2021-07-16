import scala.concurrent.duration._

enablePlugins(MdToSourcePlugin)

scalaVersion := "3.0.1-RC2"

resolvers += Resolver.JCenterRepository

MdToSource / mdDirectory := file("manuscript")

val zioVersion = "1.0.9"

libraryDependencies ++= Seq(
  "org.jetbrains" % "annotations-java5" % "15.0",
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "org.scalameta" %% "munit" % "0.7.26" % Test
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
testFrameworks += new TestFramework("munit.Framework")

val testPar: sbt.TaskKey[scala.Unit] = TaskKey("runAll")

testPar := Def.taskDyn {
  val allMainTasks = (Compile / discoveredMainClasses).value.map { m =>
    (Compile / runMain).toTask(s" $m")
  }
  Def.sequential(allMainTasks)
}.value

scalacOptions += "-Yexplicit-nulls"
scalacOptions -= "-explain-types"
scalacOptions -= "-explain"
/*
  Some options:
    1. Hub Examples
    2. Crypto Examples
    3. GitHub Action improvements
      - Recognize when `runAll` is happening over on Github, and use a fake console
    4. Something totally different
 */
