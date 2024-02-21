package streams

import zio.stream.*

trait CommitStream:
  def commits: Stream[Nothing, Commit]

case class Commit(
    project: Project,
    author: Author,
    message: String,
    added: Int,
    removed: Int
)

object CommitStream:
  object Live extends CommitStream:
    def commits: Stream[Nothing, Commit] =
      ZStream.repeatZIO(randomCommit)

  private val randomCommit =
    defer {
      val author =
        Author.random.run
      val project =
        Project.random.run
      val message =
        Message.random.run
      val linesAdded =
        Random.nextIntBounded(500).run
      val linesRemoved =
        Random.nextIntBounded(500).run
      Commit(
        project,
        author,
        message,
        linesAdded,
        -linesRemoved
      )
    }
end CommitStream

object Message:
  private val generic =
    List(
      "Refactor code",
      "Add documentation",
      "Update dependencies",
      "Format code",
      "Fix bug",
      "Add feature",
      "Add tests",
      "Remove unused code"
    )

  def random: ZIO[Any, Nothing, String] =
    randomElementFrom(generic)

case class Project(
    name: String,
    language: Language
)
object Project:
  private val entries =
    List(
      Project("ZIO", Language.Scala),
      Project("Tapir", Language.Scala),
      Project("Kafka", Language.Java),
      Project("Flask", Language.Python),
      Project("Linux", Language.C)
    )

  val random: ZIO[Any, Nothing, Project] =
    randomElementFrom(entries)

enum Language:
  case Scala,
    Java,
    C,
    CPlusPlus,
    Go,
    Rust,
    Python,
    Unison,
    Ruby

enum Author:
  case Kit,
    Adam,
    Bruce,
    James,
    Bill

object Author:
  val random: ZIO[Any, Nothing, Author] =
    randomElementFrom(Author.values.toList)
