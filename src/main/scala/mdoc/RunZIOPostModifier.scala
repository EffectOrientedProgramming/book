package mdoc

class RunZIOPostModifier extends PostModifier:
  val name =
    "runzio"
  def process(ctx: PostModifierContext): String =
    ctx.outputCode
