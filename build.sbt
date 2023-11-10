import BuildTooling.*

name := "EffectOrientedProgramming"

inThisBuild(scalaVersion := "3.3.1")

initialize := {
  initialize.value
  val required = VersionNumber("11")
  val current = VersionNumber(sys.props("java.specification.version"))
  assert(current.get(0).get >= required.get(0).get, s"Java $required or above required")
}

// This tells mdoc which folder to analyze
mdocIn := file("Chapters")
//mdocIn := file("ChaptersTiny")
// This is where the generated markdown files will be placed,
// after the scala blocks has been compiled/executed
mdocOut := file("manuscript")

// Tells our example extraction code where to find the examples
mdDir := file("Chapters")
// Tells our example extraction code where to put the extracted examples
examplesDir := file("Examples/src/main/scala")

examplesHelperDir := file("Examples/mdoctools/src/main/scala")

lazy val illustratedPrimer =
  (project in file("illustratedPrimer"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies ++= List(
        "com.raquo" %%% "laminar" % "15.0.0",
        "com.raquo" %%% "waypoint" % "6.0.0"
      )
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
    libraryDependencies ++= experimentLibrariesThatNeedToBeVettedForInclusionInBook
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

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val mdoctools = (project in file("mdoctools"))
  .settings(commonSettings)

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

lazy val bookTxt = taskKey[Unit]("Create the Book.txt")

bookTxt := generateBookTxtFromNumberedChapters(mdocIn.value, mdocOut.value)

mdoc := mdoc.dependsOn(bookTxt).evaluated

lazy val cleanManuscript = taskKey[Unit]("Clean manuscript dir")

cleanManuscript := IO.delete(mdocOut.value) // TODO Consider moving raw file IO to BuildTooling

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
  mdoc.evaluated
  produceLeanpubManuscript(mdocOut.value)
}

generateExamples := generateExamplesTask.value
Global / onChangedBuildSource := ReloadOnSourceChanges

// windows sometimes produces OverlappingFileLockException
scalafmtOnCompile := (!System.getProperty("os.name").toLowerCase.contains("win"))
