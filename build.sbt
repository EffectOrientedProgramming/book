name := "EffectOrientedProgramming"

inThisBuild(scalaVersion := "3.3.0")


// This tells mdoc which folder to analyze
mdocIn := file("Chapters")
// This is where the generated markdown files will be placed,
// after the scala blocks has been compiled/executed
mdocOut := file("manuscript")

import BuildTooling._
// Tells our example extraction code where to find the examples
mdDir := file("Chapters")
// Tells our example extraction code where to put the extracted examples
examplesDir := file("Examples/src/main/scala")

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
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= experimentLibrariesThatNeedToBeVettedForInclusionInBook
  )

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val root =
  (project in file("."))
    .settings(commonSettings)
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
