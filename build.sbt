import scala.concurrent.duration._

enablePlugins(MdToSourcePlugin)

scalaVersion := "3.0.0"

resolvers += Resolver.JCenterRepository

MdToSource / mdDirectory := file("Chapters")

val zioVersion = "1.0.8"

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
// bill input
  Def.sequential(allMainTasks)
}.value

scalacOptions += "-Yexplicit-nulls"
scalacOptions -= "-explain-types"
scalacOptions -= "-explain"
