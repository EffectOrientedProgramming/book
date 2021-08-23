package typeclasses.plagiarism

case class LiteraryWork(
    content: String,
    citations: Set[String]
)

trait Scannable[T]:

  extension (t: T)
    def literaryWork(): LiteraryWork

def isLikelyPlagiarised[T: Scannable](
    t: T
): Boolean = t.literaryWork().citations.size == 0

case class TermPaper(content: String)

case class PhdThesis(
    abstract_ : String,
    advisor: String,
    citations: Set[String]
)

case class ResearchPaper(
    abstract_ : String,
    coAuthors: Seq[String]
)

given Scannable[TermPaper] with

  extension (t: TermPaper)
    def literaryWork(): LiteraryWork =
      LiteraryWork(t.content, Set.empty)

given Scannable[PhdThesis] with

  extension (t: PhdThesis)
    def literaryWork(): LiteraryWork =
      LiteraryWork(t.abstract_, t.citations)

// TODO Scannable[ResearchPaper]

@main
def scanForPlagiarism() =
  assert(
    isLikelyPlagiarised(
      TermPaper("I am unique!")
    ) == true
  )

  assert(
    isLikelyPlagiarised(
      PhdThesis(
        "New bit of research",
        advisor = "ProfessorX",
        citations = Set("Wikipedia")
      )
    ) == false
  )
