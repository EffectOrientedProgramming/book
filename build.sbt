name := "EffectOrientedProgramming"

// This tells mdoc which folder to analyze
mdocIn := file("Chapters")
// This is where the generated markdown files will be placed,
// after the scala blocks has been compiled/executed
mdocOut := file("manuscript")

import BuildTooling._
mdDir := file("Chapters")
examplesDir := file("Examples/src/main/scala")

lazy val booker =
  (project in file("booker"))
    .dependsOn(experiments)
    .settings(commonSettings)
    .enablePlugins(GraalVMNativeImagePlugin)

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

Compile / packageDoc / publishArtifact := false

Compile / doc / sources := Seq.empty

lazy val bookTxt = taskKey[Unit]("Create the Book.txt")

bookTxt := generateBookTxtFromNumberedChapters(mdocIn.value, mdocOut.value)

mdoc := mdoc.dependsOn(bookTxt).evaluated

lazy val cleanManuscript = taskKey[Unit]("Clean manuscript dir")

cleanManuscript := IO.delete(mdocOut.value)

clean := clean.dependsOn(cleanManuscript).value

lazy val formatAndCompileCode = taskKey[Unit]("Make manuscript")

formatAndCompileCode := formatAndCompileCode
  .dependsOn(
    Compile / scalafmt,
    booker / Compile / scalafmt,
    experiments / Compile / compile,
    experiments / Compile / scalafmt,
  )

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
