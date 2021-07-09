object KotlinConversion {

  def convert(line: String): Option[String] = {
    if (isOnlyAClosingBrace(line))
      None
    else
      Some(
        if (lineIsQuoted(line) || lineIsPartOfATemplate(line))
          line
        else if (isTheBeginningOfAClass(line))
          replaceOpeningBraceWithColon(line)
        else if (isTheBeginningOfAnInterface(line))
          convertInterfaceToTrait(
            replaceOpeningBraceWithColon(line)
          )
        else if (isAFunctionSignatureWithAnEmptyBody(line))
          putEqualsSignBeforeEmptyBody(line)
        else if (
          isAFunctionSignature(line) || isTheEndOfAFunctionSignature(line)
        )
          convertFunctionSignature(line)
        else if (isAnIf(line) || isAFor(line) || isAWhile(line))
          eliminateOpeningBrace(line)
        else if (isElseFollowingABrace(line))
          eliminateClosingBraceBeforeElse(line)
        else if (isElseFlankedByBraces(line))
          eliminateBracesFlankingElse(line)
        else if (isElseBeforeABrace(line))
          eliminateOpeningBraceAfterElse(line)
        else
          line
      )
  }.map(convertEnum) // .map(convertAnglesToSquares) // TODO reimplement
    .map(convertReversed)

  def convertReversed(line: String): String = {
    println("converting reversed()")
    line.replace("reversed()", "reverse")
  }

  def convertEnum(line: String): String =
    line.replace("enum class", "enum")

  def convertAnglesToSquares(line: String): String = {
    if (line.contains("<span")) // it's html, leave it alone
      line
    else {
      // TODO confirm balanced braces
      // TODO act only if region inside braces contains only: letters < , > *
    }
    ???
  }

  def isElseFollowingABrace(line: String): Boolean =
    line.trim.equals("} else")

  def eliminateClosingBraceBeforeElse(line: String): String =
    line.replace("} else", "else")

  def isElseBeforeABrace(line: String): Boolean =
    line.trim.equals("else {")

  def eliminateOpeningBraceAfterElse(line: String): String =
    line.replace("else {", "else")

  def isAFunctionSignatureWithAnEmptyBody(line: String): Boolean =
    isAFunctionSignature(line) && line.contains("{}")

  def putEqualsSignBeforeEmptyBody(line: String): String =
    line.replace("{}", "= {}")

  def lineIsPartOfATemplate(line: String): Boolean =
    line.contains("${")

  def lineIsQuoted(line: String): Boolean =
    line.trim.startsWith("\"")

  def eliminateBracesFlankingElse(line: String): String =
    line.replace("} else {", "else")

  def isElseFlankedByBraces(line: String): Boolean =
    line.trim.equals("} else {")

  def isAFunctionSignature(line: String): Boolean =
    line.contains("def") && line.contains("{") && !line.contains("=") &&
      !line.contains("{{")

  def isTheEndOfAFunctionSignature(line: String): Boolean =
    (line.trim.startsWith("):") || line.trim.startsWith(") :")) && line.trim
      .endsWith("{") && !line.contains("=") &&
      !line.contains("{{")

  def convertFunctionSignature(line: String): String =
    replaceOpeningBraceWithEqualsSign(line)

  def isOnlyAClosingBrace(line: String) =
    line.trim.equals("}")

  def isAnIf(line: String): Boolean =
    line.contains("if") && line.contains("(") && line.contains(")") && line.trim
      .endsWith("{")

  def isTheBeginningOfAClass(line: String): Boolean =
    (line.trim.startsWith("class")
      || line.trim.startsWith("abstract class")
      || line.trim.startsWith("sealed class")
      || line.trim.startsWith("enum class")
      || line.trim.startsWith("open class")) && line.trim.endsWith("{")

  def isTheBeginningOfAnInterface(line: String): Boolean =
    line.trim.startsWith("interface") && line.trim.endsWith("{")

  def replaceOpeningBraceWithColon(line: String): String =
    line.replace(" {", ":")

  def convertInterfaceToTrait(line: String): String =
    line.replace("interface", "trait")

  def isAFor(line: String): Boolean =
    line.trim.startsWith("for (") && line.contains(")") && line.contains("{")

  def isAWhile(line: String): Boolean =
    line.trim.startsWith("while (") && line.contains(")") && line.contains("{")

  def replaceOpeningBraceWithEqualsSign(line: String) =
    line.replace("{", "=")

  def eliminateOpeningBrace(line: String) =
    line.replace("{", "")
}
