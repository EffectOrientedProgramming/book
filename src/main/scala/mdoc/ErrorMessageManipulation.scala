package mdoc

object ErrorMessageManipulation {
  def cleanupZioErrorOutput(raw: String) =
    // TODO clean up initial error: bit and final carat'ed indicator when possible
    val filtered =
      raw
        .split("\n")
        // TODO This might be safer to do after the lower Mdoc (case sensitive) replacement
        .filter(line => !line.contains("mdoc"))

    val lastLine =
      filtered
        .last

    val withoutTrailingBlankLine =
      if (lastLine.isBlank)
        filtered.init
      else
        filtered

    val effectCantFailMsg =
      """
        |This error handling operation assumes your effect can fail. However, your effect has Nothing for the error type, which means it cannot fail, so there is no need to handle the failure. To find out which method you can use instead of this operation, please see the reference chart at: https://zio.dev/can_fail.
        |I found:
        |
        |    CanFail.canFail[E](/* missing */summon[scala.util.NotGiven[E =:= Nothing]])
        |
        |But no implicit values were found that match type scala.util.NotGiven[E =:= Nothing].
        |""".stripMargin

    val effectCantFailMsgSimple =
      """
        |This error handling operation assumes your effect
        |can fail. However, your effect has Nothing for the
        |error type, which means it cannot fail, so there
        |is no need to handle the failure.
        |""".stripMargin

    val canOnlyCallRunOnZiosMsg =
      """value run is not a member of Int.
        |An extension method was tried, but could not be fully constructed:
        |
        |    run[R, E, A](1.+(1))
        |
        |    failed with:
        |
        |        Found:    (2 : Int)
        |        Required: ZIO[Nothing, Any, Any]
        |                                  ^""".stripMargin
    val canOnlyCallRunOnZiosMsgSimple =
      """value run is not a member of Int.""".stripMargin


    withoutTrailingBlankLine
      .mkString("\n")
      .replace(effectCantFailMsg, effectCantFailMsgSimple)
      .replace(canOnlyCallRunOnZiosMsg, canOnlyCallRunOnZiosMsgSimple)
      .replace(
        "error:\n\n\n──── ZLAYER ERROR ────────────────────────────────────────────────────",
        "──── ZLAYER ERROR ───────────"
      )
      .replace(
        "──────────────────────────────────────────────────────────────────────",
        "─────────────────────────────"
      )
      .replace("repl.MdocSession.MdocApp.", "")
      .replace(
        "Please provide a layer for the following type",
        "Please provide a layer for"
      )
  end cleanupZioErrorOutput

}
