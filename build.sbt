import scala.util.Try
import java.io.File
import java.nio.file.{Files, Path}

resolvers += Resolver.sonatypeRepo("snapshots")

enablePlugins(MdocPlugin)
enablePlugins(GraalVMNativeImagePlugin)
enablePlugins(MdToSourcePlugin)

name := "EffectOrientedProgramming"

scalaVersion := "3.1.0"

scalacOptions += "-Yexplicit-nulls"
scalacOptions -= "-explain-types"
scalacOptions -= "-explain"
// TODO Make sure this only happens in Intellij. It breaks VSCode
// scalacOptions -= "-encoding"

val zioVersion = "2.0.0-M3"

libraryDependencies ++=
  Seq(
    // Syncronous JDBC Modules
  "io.getquill" %% "quill-jdbc" % "3.7.2.Beta1.4",
  "ch.qos.logback" % "logback-classic"                  % "1.2.6",
  // Or ZIO Modules
  "io.getquill" %% "quill-jdbc-zio" % "3.7.2.Beta1.4",
  // Postgres Async
  "io.getquill" %% "quill-jasync-postgres" % "3.7.2.Beta1.4",
    "org.jetbrains" % "annotations-java5" %
      "22.0.0",
    "org.scalameta"      %
      "scalafmt-dynamic" % "3.0.6" cross
      CrossVersion.for3Use2_13,
    "dev.zio"     %% "zio"    % zioVersion,
    "com.typesafe" % "config" % "1.4.1",
    //     cross CrossVersion.for3Use2_13,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion %
      "it,test",
    "org.scalameta" %% "munit" % "0.7.29" % Test,
    "io.circe"  % "circe-core_3"  % "0.15.0-M1",
    "io.circe" %% "circe-generic" % "0.15.0-M1",
    "com.softwaremill.sttp.client3" %% "circe" %
      "3.3.16",
    "com.softwaremill.sttp.client3" %% "core" %
      "3.3.16",

    "org.apache.kafka" % "kafka-clients" % "3.0.0",

    "org.testcontainers"    % "testcontainers"            % "1.16.2",
    "org.testcontainers"    % "postgresql"            % "1.16.2",
    "org.testcontainers"    % "kafka"            % "1.16.2",
    "org.testcontainers"    % "mockserver"            % "1.16.2",
    "org.testcontainers"    % "toxiproxy"            % "1.16.2",
    "io.github.arkinator" % "mockserver-client-java" % "5.11.7",
    "org.postgresql" % "postgresql" % "42.3.1"
    
//  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.10.5"

// "io.d11" %% "zhttp" % "1.0.0.0-RC17", //
    // TODO Check for updates supporting ZIO2
    // milestones
// "io.d11" %% "zhttp-test" % "1.0.0.0-RC17"
    // % Test,
//    "dev.zio" %% "zio-json" % "0.2.0-M1"
  )

configs(IntegrationTest)

Defaults.itSettings




testFrameworks +=
  new TestFramework(
    "zio.test.sbt.ZTestFramework"
  )

testFrameworks +=
  new TestFramework("munit.Framework")

mdocIn := file("Chapters")

mdDir := file("Chapters")

examplesDir := file("Examples/src/main/scala")

mdocOut := file("manuscript")

// windows sometimes produces OverlappingFileLockException
scalafmtOnCompile := (!System.getProperty("os.name").toLowerCase.contains("win"))

Compile / packageDoc / publishArtifact := false

Compile / doc / sources := Seq.empty


// for building in a docker container
//graalVMNativeImageGraalVersion := Some("21.2.0")

GraalVMNativeImage / mainClass := Some("booker.run")

graalVMNativeImageCommand := (
  if (System.getProperty("os.name").toLowerCase.contains("win")) {
    val f = Try(file(System.getenv("JAVA_HOME")) / "lib" / "svm" / "bin" / "native-image.exe")
    f.filter(_.exists()).fold(_ => "native-image.exe", _.absolutePath)
  } else {
    val f = Try(file(System.getenv("JAVA_HOME")) / "lib" / "svm" / "bin" / "native-image")
    f.filter(_.exists()).fold(_ => "native-image", _.absolutePath)
  }
)

graalVMNativeImageOptions ++= (
  if (!System.getProperty("os.name").toLowerCase.contains("mac"))
    { Seq("--static") }
  else
    { Seq.empty }
)

graalVMNativeImageOptions ++= Seq(
  "--verbose",
  "--no-fallback",
  "--install-exit-handlers",
  "-H:+ReportExceptionStackTraces",
  "-H:Name=booker",
)

/*
// for generating graalvm configs
run / fork := true

run / javaOptions += s"-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image"
 */

lazy val bookTxt = taskKey[Unit]("Create the Book.txt")

bookTxt := {

  import scala.util.Try
  import scala.collection.JavaConverters._

  val files = Files.list(mdocIn.value.toPath).iterator().asScala

  def chapterNum(path: Path): Option[Int] = {
    val justFile = path.toFile.getName.split(File.pathSeparator).last
    justFile.split('_').headOption.flatMap { firstPart =>
      Try(firstPart.toInt).toOption
    }
  }

  val chapters = files.flatMap { f =>
    if (f.toFile.ext == "md") {
      chapterNum(f).map(_ -> f)
    }
    else
    {
      None
    }
  }.toSeq.sortBy(_._1).map(_._2.toFile.getName.stripPrefix(mdocIn.value.getName).stripPrefix(File.pathSeparator))

  val bookTxtPath = mdocOut.value / "Book.txt"
  mdocOut.value.mkdir()
  Files.write(bookTxtPath.toPath, chapters.asJava)
}

mdoc := mdoc.dependsOn(bookTxt).evaluated
