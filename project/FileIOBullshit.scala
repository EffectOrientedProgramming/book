import sbt.IO
import sbt.*
import Keys.*
import java.io.File
import java.nio.file.Path
import java.nio.file.{
  Files,
  Path,
  Paths,
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

  def copyFolder(src: Path, dest: Path): Unit =
    scalaFileWalk(src).foreach(source =>
      FileIOBullshit.copy(
        source,
        dest.resolve(src.relativize(source))
      )
    )

  def scalaFileWalk(src: Path): List[Path] = {
    val currentLevelFiles =
      Files.list(src).iterator().asScala.toList
    currentLevelFiles ++
      currentLevelFiles.flatMap(file =>
        if (file.toFile.isDirectory)
          scalaFileWalk(file)
        else
          List.empty
      )
  }

  def deleteAllScalaFilesRecursively(
      file: File
  ): Unit = {
    import java.io.{File, FilenameFilter}
    val onlyScala: FilenameFilter =
      (_: File, name: String) =>
        name.endsWith(".scala")
    file
      .listFiles(onlyScala)
      .toSeq
      .foreach(_.delete())
    file
      .listFiles()
      .foreach(file =>
        if (file.isDirectory)
          deleteAllScalaFilesRecursively(file)
      )
    if (
      file.isDirectory &&
      file.listFiles().isEmpty
    )
      file.delete()
  }

  def filesIn(p: Path): List[Path] =
    Files.walk(p).iterator().asScala.toList

  def markdownFilesIn(p: Path): List[Path] =
    filesIn(p).filter(_.toFile.ext == "md")

  def scalaFilesIn(p: Path): List[Path] =
    filesIn(p).filter(_.toFile.ext == "scala")

  def markdownFilesInFile(f: File) =
    f.listFiles().filter(_.ext == "md")
}
