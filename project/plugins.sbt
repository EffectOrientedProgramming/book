addSbtPlugin(
  "org.typelevel" % "sbt-tpolecat" % "0.5.0"
)

addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.5.2"
)

addSbtPlugin(
  "org.scalameta" % "sbt-mdoc" % "2.5.2"
)

addSbtPlugin(
  "nl.gn0s1s" % "sbt-dotenv" % "3.0.0"
)

addSbtPlugin(
  "com.github.sbt" % "sbt-native-packager" %
    "1.9.16"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-bloop" % "1.5.16"
)

//lazy val MdToSourcePlugin =
//  RootProject(file("../MdToSourcePlugin"))

//lazy val root = (project in file("."))
//  .dependsOn(MdToSourcePlugin)

libraryDependencies +=
  "org.scalameta" %% "mdoc" % "2.5.2"

ThisBuild / libraryDependencySchemes ++=
  Seq(
    "org.scala-lang.modules" %% "scala-xml" %
      VersionScheme.Always
  )

addSbtPlugin(
  "org.scala-js" % "sbt-scalajs" % "1.16.0"
)
