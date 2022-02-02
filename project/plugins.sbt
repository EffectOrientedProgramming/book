resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin(
  "io.github.davidgregory084" % "sbt-tpolecat" %
    "0.1.20"
)
addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
)
addSbtPlugin(
  "org.scalameta" % "sbt-mdoc" % "2.3.0"
)

addSbtPlugin(
  "au.com.onegeek" % "sbt-dotenv" % "2.1.233"
)

addSbtPlugin(
  "com.github.sbt" % "sbt-native-packager" %
    "1.9.7"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-bloop" %
    "7d2bf0af+20171218-1522"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-scalafix" % "0.9.34"
)

//lazy val MdToSourcePlugin =
//  RootProject(file("../MdToSourcePlugin"))

//lazy val root = (project in file("."))
//  .dependsOn(MdToSourcePlugin)

libraryDependencies +=
  "org.scalameta" %% "mdoc" % "2.2.23"
