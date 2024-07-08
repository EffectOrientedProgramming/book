//addSbtPlugin(
//  "org.typelevel" % "sbt-tpolecat" % "0.5.0"
//)

addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.5.2"
)

addSbtPlugin(
  "org.scalameta" % "sbt-mdoc" % "2.5.3"
)

addSbtPlugin(
  "nl.gn0s1s" % "sbt-dotenv" % "3.0.0"
)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-bloop" % "1.5.18"
)

//ThisBuild / libraryDependencySchemes ++=
//  Seq(
//    "org.scala-lang.modules" %% "scala-xml" %
//      VersionScheme.Always
//  )

addSbtPlugin(
  "org.scala-js" % "sbt-scalajs" % "1.16.0"
)

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
