resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin(
  "io.github.davidgregory084" % "sbt-tpolecat" %
    "0.1.20"
)
addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.4.3"
)
addSbtPlugin(
  "org.scalameta" % "sbt-mdoc" %
    "2.2.24"
)

addSbtPlugin(
  "au.com.onegeek" % "sbt-dotenv" % "2.1.233"
)

addSbtPlugin(
  "com.github.sbt" % "sbt-native-packager" %
    "1.9.6"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-bloop" % "1.4.11"
)

//lazy val MdToSourcePlugin =
//  RootProject(file("../MdToSourcePlugin"))

//lazy val root = (project in file("."))
//  .dependsOn(MdToSourcePlugin)

libraryDependencies +=
  "org.scalameta" %% "mdoc" % "2.2.23"