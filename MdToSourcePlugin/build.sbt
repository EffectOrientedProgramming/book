sbtPlugin := true

libraryDependencies += "org.scalameta" %% "munit" % "0.7.20" % Test
libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % "2.4.0"

testFrameworks += TestFramework("munit.Framework")
