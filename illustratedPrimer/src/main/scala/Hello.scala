import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

val nameVar: Var[String] = Var(initial = "*Choose a topic*")

object KnownTopic:
  val topics =
    Set("concurrency", "time", "environment")
  def apply(topic: String) =
    span(
      cls := "known-topic",
      onClick.mapTo(topic.toLowerCase) --> nameVar,
      topic + " "
    )

  // TODO Handle symbols
  def recognize(content: String) =
    val words = content.split(" ")
    val recognized = words.map { word =>
      if topics.contains(word.toLowerCase) then KnownTopic(word)
      else span(word + " ")
    }
    recognized



object ActionPanel:
  def apply(topic: Var[String]) =
    div(
      cls := "action-panel",
      div(
        child.text <-- topic
      ),
      div("More info"),
      div("Examples"),
      div("etc")
    )

val rootElement = div(
  div(
    KnownTopic.recognize(
    """This is a much more flexible approach, where every time a known topic
      |is mentioned, it is recognized and turned into a link. So if we talk about
      | accessing environment variables, then environment will be highlighted and
      | interactive each time.
    """.stripMargin
    )
  ),
  ActionPanel(nameVar),

)

// In most other examples, containerNode will be set to this behind the scenes
val containerNode = dom.document.querySelector("#laminar-app")

object Hello extends App {
  println("doing stuff")
  render(containerNode, rootElement)
}