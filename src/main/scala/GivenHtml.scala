object GivenHtml {}

case class Identifier(content: String)
case class HtmlFrag(content: String)

case class StringField(
    identifier: Identifier,
    content: String
)

case class StringChoices(
    identifier: Identifier,
    content: List[String]
)

trait HtmlRep[A]:
  extension (a: A) def formField(): HtmlFrag

given HtmlRep[StringField] with

  extension (a: StringField)

    def formField(): HtmlFrag =
      HtmlFrag(
        s"""<input type="text" id="${a.identifier.content}" name="${a.identifier.content}" value=${a.content}>"""
      )

trait RadioOption[A]:
  extension (a: A) def optField(): HtmlFrag

given RadioOption[StringField] with

  extension (a: StringField)

    def optField(): HtmlFrag =
      // TODO Name should not be identifier.
      HtmlFrag(
        s"""<input type="radio" id="${a.identifier.content}" name="${a.identifier.content}" value=${a.content}>"""
      )

given (using
    RadioOption[StringField]
): RadioOption[List[StringField]] with

  extension (a: List[StringField])

    def optField(): HtmlFrag =
      HtmlFrag(
        "<form> " +
          a.map(_.optField().content)
            .mkString("\n") +
          "</form>"
      )

//  HtmlFrag(s"""<input type="radio" id="${a.identifier.content}" name="${a.identifier.content}" value=${a.content}>""")

//given HtmlRep[List[StringField]] with
//  extension (a: List[StringField]) def formField(): HtmlFrag =
//    a.map(_.formField())
//    HtmlFrag(s"""<input type="text" id="${a.identifier.content}" name="${a.identifier.content}" value=${a.content}>""")
