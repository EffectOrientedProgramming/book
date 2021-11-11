import scala.util.Try

// for building in a docker container
//graalVMNativeImageGraalVersion := Some("21.2.0")

GraalVMNativeImage / mainClass := Some("booker.run")

graalVMNativeImageCommand := (
  if (System.getProperty("os.name").toLowerCase.contains("win")) {
    val f = Try(file(System.getenv("JAVA_HOME")) / "lib" / "svm" / "bin" / "native-image.exe")
    f.filter(_.exists()).fold(_ => "native-image.exe", _.absolutePath)
  } else {
    val f = Try(file(System.getenv("JAVA_HOME")) / "lib" / "svm" / "bin" / "native-image")
    f.filter(_.exists()).fold(_ => "native-image", _.absolutePath)
  }
)

graalVMNativeImageOptions ++= (
  if (!System.getProperty("os.name").toLowerCase.contains("mac"))
    { Seq("--static") }
  else
    { Seq.empty }
)

graalVMNativeImageOptions ++= Seq(
  "--verbose",
  "--no-fallback",
  "--install-exit-handlers",
  "-H:+ReportExceptionStackTraces",
  "-H:Name=booker",
)

/*
// for generating graalvm configs
run / fork := true

run / javaOptions += s"-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image"
 */
