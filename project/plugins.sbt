resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin(
  "io.github.davidgregory084" % "sbt-tpolecat" %
    "0.2.2"
)
addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
)
addSbtPlugin(
  "org.scalameta" % "sbt-mdoc" % "2.3.2"
)

addSbtPlugin(
  "nl.gn0s1s" % "sbt-dotenv" % "3.0.0"
)

addSbtPlugin(
  "com.github.sbt" % "sbt-native-packager" %
    "1.9.9"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-bloop" % "7e30aec7"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-scalafix" % "0.10.0"
)

//lazy val MdToSourcePlugin =
//  RootProject(file("../MdToSourcePlugin"))

//lazy val root = (project in file("."))
//  .dependsOn(MdToSourcePlugin)

libraryDependencies +=
  "org.scalameta" %% "mdoc" % "2.2.23"
