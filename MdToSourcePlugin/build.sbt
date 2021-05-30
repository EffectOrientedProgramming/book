sbtPlugin := true

libraryDependencies += "org.scalameta" %% "munit" % "0.7.20" % Test

testFrameworks += TestFramework("munit.Framework")
