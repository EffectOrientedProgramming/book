resolvers ++=
  Resolver.sonatypeOssRepos("snapshots")

addSbtPlugin(
  "io.github.davidgregory084" % "sbt-tpolecat" %
    "0.4.4"
)
addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.5.0"
)

addSbtPlugin(
  "org.scalameta" % "sbt-mdoc" % "2.3.7"
)

addSbtPlugin(
  "nl.gn0s1s" % "sbt-dotenv" % "3.0.0"
)

addSbtPlugin(
  "com.github.sbt" % "sbt-native-packager" %
    "1.9.16"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-bloop" % "1.5.7"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-scalafix" % "0.11.0"
)

//lazy val MdToSourcePlugin =
//  RootProject(file("../MdToSourcePlugin"))

//lazy val root = (project in file("."))
//  .dependsOn(MdToSourcePlugin)

libraryDependencies +=
  "org.scalameta" %% "mdoc" % "2.2.23"

ThisBuild / libraryDependencySchemes ++=
  Seq(
    "org.scala-lang.modules" %% "scala-xml" %
      VersionScheme.Always
  )

addSbtPlugin(
  "org.scala-js" % "sbt-scalajs" % "1.13.2"
)
