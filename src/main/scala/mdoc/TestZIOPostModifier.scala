package mdoc

class TestZIOPostModifier extends PostModifier:
  val name = "testzio"

  def process(ctx: PostModifierContext): String =
    ctx.outputCode
