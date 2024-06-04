enablePlugins(MdocPlugin)

name := "EffectOrientedProgramming"

initialize := {
  initialize.value
  val required = VersionNumber("11")
  val current = VersionNumber(sys.props("java.specification.version"))
  assert(current.get(0).get >= required.get(0).get, s"Java $required or above required")
}

scalaVersion := "3.4.2"

// This tells mdoc which folder to analyze
mdocIn := file("Chapters")
//mdocIn := file("ChaptersTiny")
// This is where the generated markdown files will be placed,
// after the scala blocks has been compiled/executed
mdocOut := file("manuscript")

// Tells our example extraction code where to put the extracted examples
val examplesDir = "examples"

val zioVersion = "2.1.2"

scalacOptions +=
  Seq(
    "java.lang",
    "scala",
    "scala.Predef",
    "zio",
    "zio.direct",
    "zio.test",
  ).mkString(
    start =
      "-Yimports:",
    sep =
      ",",
    end =
      ""
  )

libraryDependencies ++=
  Seq(
    "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    "dev.zio" %% "zio"       % zioVersion,
    "dev.zio" %% "zio-cache" % "0.2.3",
    "dev.zio" %% "zio-config" % "4.0.2",
    "dev.zio" %% "zio-config-magnolia" % "4.0.2",
    "dev.zio" %% "zio-config-typesafe" % "4.0.2",
    "dev.zio" %% "zio-direct" % "1.0.0-RC7" excludeAll // to resolve conflicts with mdoc
          (
            "com.geirsson",
            "metaconfig-typesafe-config"
          ) excludeAll
          ("com.geirsson", "metaconfig-core"),
    "nl.vroste" %% "rezilience"   % "0.9.4",
    "org.scalameta" %% "mdoc" % "2.5.2",
    "dev.zio"   %% "zio-test" % zioVersion,
    "dev.zio"   %% "zio-test-sbt" % zioVersion % Test,
  )

fork := true
connectInput := true
Compile / packageDoc / publishArtifact := false
Compile / doc / sources := Seq.empty

lazy val bookTxt = taskKey[Unit]("Create the Book.txt")
bookTxt := BuildTooling.generateBookTxtFromNumberedChapters(mdocIn.value, mdocOut.value)

lazy val cleanManuscript = taskKey[Unit]("Clean manuscript dir")
cleanManuscript := IO.delete(mdocOut.value)

clean := clean.dependsOn(cleanManuscript).value

lazy val formatAndCompileCode = taskKey[Unit]("Make manuscript")

formatAndCompileCode := Def.sequential(
  Compile / scalafmt,
).value

// TODO define inputKey entirely by depending on other inputKeys
lazy val genManuscript = inputKey[Unit]("Make manuscript")

genManuscript := {
  formatAndCompileCode.value
  cleanManuscript.value
  mdocRun.value
  BuildTooling.produceLeanpubManuscript(mdocOut.value)
}

lazy val mdocRun = taskKey[Unit]("mdoc run")
mdocRun := Def.taskDyn {
  Def.task {
    bookTxt.value
    (Compile / runMain).toTask(s" mdoc.mdocRun $examplesDir").value
  }
}.value

import complete.DefaultParsers._
lazy val mdocRunForce = inputKey[Unit]("mdoc run with force")
mdocRunForce := Def.inputTaskDyn {
  bookTxt.value
  val arg: String = spaceDelimited("<arg>").parsed.head
  Def.task {
    (Compile / runMain).toTask(s" mdoc.mdocRunForce $examplesDir $arg").value
  }
}.evaluated

lazy val mdocRunForceAll = taskKey[Unit]("mdoc run with force")
mdocRunForceAll := Def.taskDyn {
  bookTxt.value
  Def.task {
    (Compile / runMain).toTask(s" mdoc.mdocRunForceAll $examplesDir").value
  }
}.value

lazy val mdocWatch = taskKey[Unit]("mdoc watch")
mdocWatch := Def.taskDyn {
  bookTxt.value
  Def.task {
    (Compile / runMain).toTask(s" mdoc.mdocWatch $examplesDir").value
  }
}.value

Global / onChangedBuildSource := ReloadOnSourceChanges

// windows sometimes produces OverlappingFileLockException
//scalafmtOnCompile := (!System.getProperty("os.name").toLowerCase.contains("win"))
scalafmtOnCompile := false
