lazy val MdToSourcePlugin = RootProject(file("../MdToSourcePlugin"))

lazy val root = (project in file(".")).dependsOn(MdToSourcePlugin)

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.16")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.2.21")
