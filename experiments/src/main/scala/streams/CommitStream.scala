package streams

import zio.*
import zio.stream.*

enum ProjectName:
  case Zio, Tapir, Kafka, Linux

enum Author:
  case Kit, Adam, Bruce, James, Bill

case class Commit(project: ProjectName, author: Author)

trait CommitStream:
  def commits: Stream[Nothing, Commit]

object CommitStream:
  object Live extends CommitStream:
    def commits: Stream[Nothing, Commit] =
      ZStream.repeatZIO(randomCommit)

  private val randomCommit =
    for
      authorIndex <- Random.nextIntBounded(Author.values.length)
      projectIndex <- Random.nextIntBounded(ProjectName.values.length)
    yield Commit(ProjectName.fromOrdinal(projectIndex), Author.fromOrdinal(authorIndex))

