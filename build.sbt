import java.io.File
import java.nio.file.{Files, Path, Paths}
import BuildTooling._

name := "EffectOrientedProgramming"

val zioVersion = "2.0.10"

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio"          % zioVersion,
    "dev.zio" %% "zio-cache"  % "0.2.2",
    "dev.zio" %% "zio-concurrent"          % zioVersion,
    "dev.zio" %% "zio-logging"  % "2.1.8",
    "dev.zio" %% "zio-streams"  % zioVersion,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    "dev.zio" %% "zio-prelude"  % "1.0.0-RC16",
  ),
  scalaVersion := "3.2.2",
  scalacOptions -= "-explain-types",
  scalacOptions -= "-explain",
)


lazy val booker = (project in file("booker")).dependsOn(experiments).settings(commonSettings).enablePlugins(GraalVMNativeImagePlugin)
lazy val experiments = (project in file("experiments"))
.settings(commonSettings)
//  .settings(fork:=true)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-text" % "1.10.0",
      "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % "0.9.0",
      "io.github.scottweaver" %% "zio-2-0-db-migration-aspect" % "0.9.0",
          "io.getquill" %% "quill-jdbc-zio" % "4.6.0.1",
      "io.getquill" %% "quill-zio" % "4.6.0.1",
      "dev.zio" %% "zio-process" % "0.7.1",
    )
  )
//lazy val rube = (project in file("rube")).settings(commonSettings)

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val root = (project in file(".")).settings(commonSettings).enablePlugins(MdocPlugin).aggregate(booker, experiments /*, rube*/)

mdocIn := file("Chapters")

mdocOut := file("manuscript")

// windows sometimes produces OverlappingFileLockException
scalafmtOnCompile := (!System.getProperty("os.name").toLowerCase.contains("win"))

Compile / packageDoc / publishArtifact := false

Compile / doc / sources := Seq.empty

lazy val bookTxt = taskKey[Unit]("Create the Book.txt")

bookTxt := generateBookTxtFromNumberedChapters(mdocIn.value, mdocOut.value)


mdoc := mdoc.dependsOn(bookTxt).evaluated

lazy val cleanManuscript = taskKey[Unit]("Clean manuscript dir")

cleanManuscript := {
  IO.delete(mdocOut.value)
}

clean := clean.dependsOn(cleanManuscript).value

lazy val genManuscript = inputKey[Unit]("Make manuscript")

genManuscript := {
  cleanManuscript.value
  val manuscript: File = mdocOut.value

  (Compile / scalafmt).value
  (booker / Compile / scalafmt).value
  (experiments / Compile / compile).value
  (experiments / Compile / scalafmt).value

  mdoc.evaluated

  import scala.jdk.CollectionConverters._

  def filesIn(p: Path) =
    Files.walk(p)
      .iterator()
      .asScala
      .toList


  case class ExperimentFile(p: Path)

  val experimentClasses: Map[String, List[ExperimentFile]] =
    filesIn(file("experiments/src").toPath)
      .filter(_.toFile.ext == "scala")
      .map(ExperimentFile)
      .groupBy { file => file.p.getParent.toString }


  val nf = manuscript / "ExperimentsSection.md"
  val experimentsHeaderContent =
    "# Experiments\n\n" +
    "These experiments are not currently attached to a chapter, but are included for previewing. Before publication, we should not have any lingering experiments here.\n\n"
  IO.append(manuscript / "Book.txt", nf.getName + "\n")

  val proseFiles: Seq[ProseFile] =
    filesIn(manuscript.toPath)
      .filter(_.toFile.getName.endsWith(".md"))
      .sortBy(_.toFile.getName)
      .map(ProseFile)

  experimentClasses.foreach {
    case (dir, dirFiles) =>
      val packagedName: String = dir.stripPrefix("experiments/src/main/scala/")

      val proseFileOnSameTopic: Option[ProseFile] =
        proseFiles.find(_.cleanName == packagedName)

      def fileFence(experimentFile: ExperimentFile) = {
        val file = experimentFile.p.toFile
        val lines = IO.read(file)
        FencedCode(s"""
          |
          |### ${file.toString}
          |```scala
          |$lines
          |```
          |""".stripMargin
        )
      }


      val allFences: List[FencedCode] =
        dirFiles.sortBy(_.p.getFileName.toString).map(fileFence)

      proseFileOnSameTopic match {
        case Some(value) =>
          appendExperimentsToMatchingProseFile(value, allFences)
        case None => {
          appendExperimentsToEndOfBookInNewChapter(packagedName, manuscript, allFences)
        }
      }
  }

}

generateExamples := generateExamplesTask.value

mdDir := file("Chapters")

examplesDir := file("Examples/src/main/scala")

Global / onChangedBuildSource := ReloadOnSourceChanges
