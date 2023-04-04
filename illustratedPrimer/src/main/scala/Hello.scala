import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import java.util.UUID
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import scala.util.Random

enum DynamicInfo:
  case VettedInfo(content: String)
  case Code(content: String)
  case ExpensiveChatInfo(content: String)
val eventBus = new EventBus[DynamicInfo]

case class Backend():
    def getVettedInfo(topic: String): Future[DynamicInfo] =
        Future.successful(DynamicInfo.VettedInfo(s"TODO Get saved ChatGPT info for $topic"))

    def codeExample(topic: String): Future[DynamicInfo] =
      Future.successful(DynamicInfo.Code(
        s"""object Demo extends ZIOAppDefault:
           |  def run =
           |     // TODO Generate code for $topic
           |
           |""".stripMargin
      ))

    def expensiveChatInfo(topic: String): Future[DynamicInfo] =
      Future.successful(DynamicInfo.ExpensiveChatInfo(
      s"TODO Hit GPT API for $topic"
    ))


enum ParagraphPiece:
  case KnownTopic(topic: String)
  case Text(text: String)

object ActionPanel:
  def apply(infoVar: Var[Option[DynamicInfo]], backend: Backend) =
    div(
      cls := "action-panel",
      Components.infoDisplay(infoVar.signal),
    )

object IllustratedPrimer:
  def apply(backend: Backend) =
    val nameVar: Var[String] = Var(initial = "*Choose a topic*")
    val infoVar: Var[Option[DynamicInfo]] = Var(None)
    val activeDropDown: Var[Option[String]] = Var(None)
    div(
      onClick --> { event =>
        activeDropDown.now() match
          case Some(value) =>
            val clickedElement: String = event.target.asInstanceOf[dom.html.Element].id
            println("Clicked element: " + clickedElement)
            if (!clickedElement.contains(value)) {
              println("Clicked outside the target element!");
              dom.document.querySelector("#" + value)
                .classList.toggle("is-hidden")
              activeDropDown.writer.onNext(None)
            }
          case None => println("No active dropdown")
            //.asInstanceOf[dom.html.Element].classList.remove("active
      },
      div(
        KnownTopic.recognize(
          """This is a much more flexible approach, where every time a known topic
            |is mentioned, it is recognized and turned into a link. So if we talk about
            | accessing environment variables, then environment will be highlighted and
            | interactive each time.
      """.stripMargin
        ).map {
          case ParagraphPiece.KnownTopic(topic) => Components.dropdownTopic(topic, infoVar.writer, backend, activeDropDown.writer)
          case ParagraphPiece.Text(text) => Components.textPiece(text)
        }
      ),
      ActionPanel(infoVar, backend),

    )

object Hello extends App {
  val backend = Backend()
  val containerNode = dom.document.querySelector("#laminar-app")
  render(containerNode, IllustratedPrimer(backend))
}