import sbt.*
import java.io.File
import java.nio.file.Path
import java.nio.file.{
  Files,
  StandardOpenOption
}

object FileIOBullshit {
  import java.nio.file.StandardCopyOption.*
  import scala.collection.JavaConverters.*

  def createFile(
                  target: File,
                  contents: Seq[String]
                ): Path = {
    target.getParentFile.mkdirs()
    Files.write(
      target.toPath,
      contents.asJava,
      StandardOpenOption.CREATE
    )
  }

  def copy(source: Path, dest: Path): Unit =
    Files.copy(source, dest, REPLACE_EXISTING)

  def filesIn(p: Path): List[Path] =
    Files.walk(p).iterator().asScala.toList

  def markdownFilesIn(p: Path): List[Path] =
    filesIn(p).filter(_.toFile.ext == "md")

}
