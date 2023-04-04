import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import concurrent.ExecutionContext.Implicits.global
import com.raquo.laminar.defs.props.HtmlProps

import java.util.UUID
import scala.util.Random

object Components extends HtmlProps:
  def knownTopic(
      topic: String,
      topicSelection: Observer[String]
  ) =
    span(
      cls := "known-topic",
      onClick.mapTo(topic.toLowerCase) -->
        topicSelection,
      topic + " "
    )

  def dropdownTopic(topic: String,
                    infoResults: Observer[Option[DynamicInfo]],
                    backend: Backend,
                    activePopover: Observer[Option[String]],
                   ) = {

    object UUIDGenerator {
      def generateRandomUUID(): String =
        Random.alphanumeric.take(10).mkString
    }

    val id = UUIDGenerator.generateRandomUUID()

    val content =
      div(
        cls := "dropdown-menu is-hidden",
//        idAttr := "dropdown-menu",
        role := "menu",
        idAttr := id,
        div(cls := "dropdown-content",
          "Actions:",
          a(href := "#", cls := "dropdown-item", "Vetted information",
            onClick.flatMap(e =>
              Signal.fromFuture(
                backend.getDynamicInfo(topic)
              )
            ) --> infoResults,
          ),
          a(href := "#", cls := "dropdown-item is-active", "Show example"),
          a(href := "#", cls := "dropdown-item", "Generate new information")
        )
      )
    //    val ariaControls: HtmlProp[String] = stringProp("aria-controls")

    span(
      cls :=  "popover-container",
      idAttr := id + "-container",
      span(
        cls := "known-topic",
        idAttr := id + "-span",
        aria.hasPopup := true,
        onClick -->
          Observer[dom.MouseEvent](onNext = ev => content.ref.classList.remove("is-hidden")),
        onClick.mapTo(Some(id))  --> activePopover,
        stringProp("aria-controls") := "dropdown-menu",
        topic + " ",
        content,
      )
    )
  }

  def textPiece(text: String) = span(text + " ")

  // TODO should not be receiving a full Var here
  def infoButton(
      topic: Var[String],
      infoResults: Observer[Option[DynamicInfo]],
      backend: Backend
  ) =
    button(
      typ := "button",
      onClick.flatMap(e =>
        Signal.fromFuture(
          backend.getDynamicInfo(topic.now())
        )
      ) --> infoResults,
      "More info"
    )

  def infoDisplay(
      info: Signal[Option[DynamicInfo]]
  ) =
    div(
      span("Dynamic info: "),
      child.text <--
        info.map {
          case Some(value) =>
            value.content
          case None =>
            "Not available yet"
        }
    )
end Components
