package booker

import tui.{TUI, TerminalApp, TerminalEvent}
import tui.view.View

object HelloWorldApp
    extends TerminalApp[Nothing, String, String]:
  override def render(s: String): View =
    View.text(s)

  override def update(
      state: String,
      event: TerminalEvent[Nothing]
  ): TerminalApp.Step[String, Nothing] =
    TerminalApp.Step.update(event.toString)

object HelloWorld extends ZIOAppDefault:
  override def run
      : ZIO[Any, Throwable, Option[String]] =
    HelloWorldApp
      .runOption("")
      .provide(TUI.live(true))
