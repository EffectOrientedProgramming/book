import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

case class DynamicInfo(content: String)
val eventBus = new EventBus[DynamicInfo]

case class Backend():
    def getDynamicInfo(topic: String): Future[DynamicInfo] =
        Future.successful(DynamicInfo(s"TODO Get ChatGPT info for $topic"))

val backend = Backend()

object Components:
  def knownTopic(topic: String, topicSelection: Observer[String]) =
    span(
      cls := "known-topic",
      onClick.mapTo(topic.toLowerCase) --> topicSelection,
      topic + " "
    )

  def textPiece(
    text: String,
               ) =
  span( text + " ")

  // TODO should not be receiving a full Var here
  def infoButton(topic: Var[String], infoResults: Observer[Option[DynamicInfo]]) =
    button(
      typ := "button",
      onClick.flatMap(e =>
        Signal.fromFuture(backend.getDynamicInfo(topic.now()))
      ) --> infoResults,
      "More info"
    )

  def infoDisplay(info: Signal[Option[DynamicInfo]]) =

    div(
      span("Dynamic info: "),
      child.text <-- info.map {
        case Some(value) => value.content
        case None => "Not available yet"
      }
    )

enum ParagraphPiece:
  case KnownTopic(topic: String)
  case Text(text: String)

object ActionPanel:
  def apply(topic: Var[String]) =
    val infoVar: Var[Option[DynamicInfo]] = Var(None)

    div(
      cls := "action-panel",
      div(
        child.text <-- topic
      ),
      Components.infoButton(topic, infoVar.writer),
      Components.infoDisplay(infoVar.signal),
      div("Examples"),
      div("etc")
    )

val rootElement = {
  val nameVar: Var[String] = Var(initial = "*Choose a topic*")
  div(
    div(
      KnownTopic.recognize(
        """This is a much more flexible approach, where every time a known topic
          |is mentioned, it is recognized and turned into a link. So if we talk about
          | accessing environment variables, then environment will be highlighted and
          | interactive each time.
    """.stripMargin
      ).map {
        case ParagraphPiece.KnownTopic(topic) => Components.knownTopic(topic, nameVar.writer)
        case ParagraphPiece.Text(text) => Components.textPiece(text)
      }
    ),
    ActionPanel(nameVar),

  )
}

// In most other examples, containerNode will be set to this behind the scenes
val containerNode = dom.document.querySelector("#laminar-app")

object Hello extends App {
  render(containerNode, rootElement)
}