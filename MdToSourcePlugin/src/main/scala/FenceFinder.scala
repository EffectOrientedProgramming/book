object FenceFinder {
  sealed trait BookScanState
  case object InAFence extends BookScanState
  case object InProse extends BookScanState

  def findFenceContents(input: String, fileName: String): Seq[CodeBlock] =
    categorizedContents(input, fileName: String)
      .collect { case block: CodeBlock =>
        block
      }

  def acceptableExampleCodeLanguage(language: String): Boolean =
    language.equals("scala") || language.equals("text") || language.equals(
      "java"
    )

  def categorizedContents(input: String, fileName: String): Seq[BookPiece] = {

    val startingPoint: (Seq[BookPiece], BookScanState, Seq[String]) =
      (Seq[CodeBlock](), InProse, Seq[String]())

    input.linesIterator.zipWithIndex
      .foldLeft(startingPoint) {

        case (
              (fencedItemsSoFar, scanState, fenceContent),
              (nextLine, lineNumber)
            ) =>
          if (nextLine.startsWith("```")) {
            scanState match {
              // TODO Stop assuming scala. Analyze this first line better.
              case InAFence => {
                (
                  fencedItemsSoFar :+ CodeBlock(
                    "scala",
                    fenceContent,
                    BookLocation(fileName, lineNumber - fenceContent.size)
                  ), // TODO shouldn't do this calculation in line
                  InProse,
                  Seq()
                )
              }
              case InProse => {
                if (acceptableExampleCodeLanguage(nextLine.drop(3))) {
                  (fencedItemsSoFar :+ Prose(fenceContent), InAFence, Seq())
                } else
                  throw new IllegalArgumentException(
                    s"Example at ${BookLocation(fileName, lineNumber + 1).clickableLink}doesn't specify a valid language: " +
                      nextLine
                  )
              }
            }
          } else
            (fencedItemsSoFar, scanState, fenceContent :+ nextLine)
      }
      ._1
  }

  def toNamedCodeBlock(codeBlock: CodeBlock): Option[NamedCodeBlock] = {
    codeBlock.content.headOption.collect {
      case s
          if s.startsWith("// ") && (s
            .endsWith(".scala") || s.endsWith(".java")) => {
        val contentThatMightIncludeOutputExpectations = codeBlock.content.tail

        NamedCodeBlock(
          s.stripPrefix("// "),
          CodeBlock(
            codeBlock.language,
            codeBlock.content.tail,
            codeBlock.location
          )
        )
      }
    }
  }

  case class CodeExample(content: Seq[String])
  case class ExpectedOutput(content: Seq[String])

  def fullCodeExampleContents(
      content: Seq[String]
  ): Either[CodeExample, (CodeExample, ExpectedOutput)] = {
    if (content.exists(_.contains("val output ="))) {
      val (codeExample, expectedOutput) =
        content.splitAt(content.indexWhere(_.contains("val output =")))
      Right(CodeExample(codeExample), ExpectedOutput(expectedOutput))
    } else Left(CodeExample(content))
  }

  //    fullCodeExampleContents(List()) match {
  //      case Left(codeOnly) => ()
  //      case Right((codeExample, expectedOutput)) => runThroughComplicatedCompiler(codeExample, expectedOutput)
  //    }

  def justCompile(codeExample: CodeExample) =
    ???

  def runThroughComplicatedCompiler(
      codeExample: CodeExample,
      expectedOutput: ExpectedOutput
  ) =
    ???

  def findNamedCodeBlocksInFile(
      input: String,
      fileName: String
  ): Seq[NamedCodeBlock] = {
    findFenceContents(input, fileName)
      .flatMap(toNamedCodeBlock)
      .filter(
        _.codeBlock.content.headOption.exists(!_.contains("{{incomplete}}"))
      )
  }
}
