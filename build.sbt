//import BuildTooling.*

name := "EffectOrientedProgramming"

// todo: 3.4.1 breaks the 06_Errors chapter
inThisBuild(scalaVersion := "3.4.1")

initialize := {
  initialize.value
  val required = VersionNumber("11")
  val current = VersionNumber(sys.props("java.specification.version"))
  assert(current.get(0).get >= required.get(0).get, s"Java $required or above required")
}

// This tells mdoc which folder to analyze
mdocIn := file("Chapters")
//mdocIn := file("ChaptersWorking")
// This is where the generated markdown files will be placed,
// after the scala blocks has been compiled/executed
mdocOut := file("manuscript")

// Tells our example extraction code where to find the examples
//mdDir := file("Chapters")
// Tells our example extraction code where to put the extracted examples
//examplesDir := file("Examples/src/main/scala")

//examplesHelperDir := file("Examples/mdoctools/src/main/scala")

val zioVersion = "2.0.21"

lazy val commonSettings =
  Seq(
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
      ),
    libraryDependencies ++=
      Seq(
        "dev.zio" %% "zio"       % zioVersion,
        "dev.zio" %% "zio-cache" % "0.2.3",
        "dev.zio" %% "zio-config" % "4.0.1",
        "dev.zio" %% "zio-config-magnolia" % "4.0.1",
        "dev.zio" %% "zio-config-typesafe" % "4.0.1",
        "dev.zio"     %%
          "zio-direct" % "1.0.0-RC7" excludeAll
          (
            "com.geirsson",
            "metaconfig-typesafe-config"
          ) excludeAll
          ("com.geirsson", "metaconfig-core"),
        "dev.zio"   %% "zio-test" % zioVersion,
        "dev.zio"   %% "zio-test-sbt" %
          zioVersion % Test,
        "nl.vroste" %% "rezilience"   % "0.9.4"
      ),
    scalacOptions -= "-explain-types",
    scalacOptions -= "-explain",
    fork := true,
    Compile / packageDoc / publishArtifact :=
      false,
    Compile / doc / sources := Seq.empty
  )

// Tool that lets us re-order numbered markdown chapters
lazy val booker =
  (project in file("booker"))
    //.dependsOn(experiments)
    .settings(commonSettings)
    .enablePlugins(GraalVMNativeImagePlugin)

// Sandbox where we can edit code with full editing capabilities, before committing to mdoc fences
lazy val experiments =
  (project in file("experiments"))
  .dependsOn(mdoctools)
  .settings(commonSettings)
  .settings(
    // TODO I put this in because a valid test is being flagged otherwise.
    scalacOptions -= "-Wunused:locals",
  )
  .settings(
    scalacOptions +=
      Seq(
        "mdoctools",
      ).mkString(
        start = "-Yimports:",
        sep = ",",
        end = ""
      ),
  )

lazy val mdoctools = (project in file("mdoctools"))
  .settings(commonSettings)
  //.settings(libraryDependencies += "org.scalameta" %% "mdoc" % "2.5.2")

lazy val root =
  (project in file("."))
    .dependsOn(mdoctools)
    .settings(commonSettings)
    .settings(
      fork := false,
      scalacOptions +=
        Seq(
          "mdoctools",
        ).mkString(
          start = "-Yimports:",
          sep = ",",
          end = ""
        ),
    )
    .enablePlugins(MdocPlugin)
    .aggregate(booker, experiments /*, rube*/)

libraryDependencies += "org.scalameta" %% "mdoc" % "2.5.2"

//lazy val chapters = project
//  .in(file("Chapters"))
//  .settings(
//    mdoc := (Compile / run).evaluated
//  )
//  .dependsOn(mdoctools)
//  .enablePlugins(MdocPlugin)

lazy val bookTxt = taskKey[Unit]("Create the Book.txt")

//bookTxt := generateBookTxtFromNumberedChapters(mdocIn.value, mdocOut.value)

//mdoc := mdoc.dependsOn(bookTxt).evaluated

lazy val cleanManuscript = taskKey[Unit]("Clean manuscript dir")

val manuscriptDir = file("manuscript")

cleanManuscript := IO.delete(manuscriptDir)

//cleanManuscript := IO.delete(mdocOut.value) // TODO Consider moving raw file IO to BuildTooling

clean := clean.dependsOn(cleanManuscript).value

lazy val formatAndCompileCode = taskKey[Unit]("Make manuscript")

formatAndCompileCode := Def.sequential(
    Compile / scalafmt,
    booker / Compile / scalafmt,
    experiments / Compile / compile,
    experiments / Compile / scalafmt,
  ).value

// TODO define inputKey entirely by depending on other inputKeys
lazy val genManuscript = inputKey[Unit]("Make manuscript")

genManuscript := {
  formatAndCompileCode.value
  cleanManuscript.value
  //mdoc.evaluated
  //produceLeanpubManuscript(mdocOut.value)
}

val examplesDir = "Examples"

lazy val mdocRun = taskKey[Unit]("mdoc run")
mdocRun := Def.taskDyn {
  Def.task {
    (Compile / runMain).toTask(s" mdoc.mdocRun $examplesDir").value
  }
}.value

lazy val mdocRunForce = taskKey[Unit]("mdoc run with force")
mdocRunForce := Def.taskDyn {
  Def.task {
    (Compile / runMain).toTask(s" mdoc.mdocRunForce $examplesDir").value
  }
}.value

lazy val mdocWatch = taskKey[Unit]("mdoc watch")
mdocWatch := Def.taskDyn {
  Def.task {
    (Compile / runMain).toTask(s" mdoc.mdocWatch $examplesDir").value
  }
}.value

//generateExamples := generateExamplesTask.value
Global / onChangedBuildSource := ReloadOnSourceChanges

// windows sometimes produces OverlappingFileLockException
//scalafmtOnCompile := (!System.getProperty("os.name").toLowerCase.contains("win"))
scalafmtOnCompile := false
