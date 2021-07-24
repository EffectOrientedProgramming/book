package scratchFiles.monads.fors

import scala.util.Random

enum Contents:
  case Stop()
  case Continue()

case class Box(contents: Contents):

  def map(
      f: Contents.Continue => Contents
  ): Box =
    contents match
      case stop: Contents.Stop =>
        this
      case continue: Contents.Continue =>
        Box(f(continue))

  def flatMap(f: Contents.Continue => Box): Box =
    contents match
      case stop: Contents.Stop =>
        this
      case continue: Contents.Continue =>
        f(continue)
end Box

object Box:

  def observe: Box =
    if Random.nextBoolean() then
      Box(Contents.Stop())
    else
      Box(Contents.Continue())

def again(continue: Contents.Continue): Box =
  Box.observe

@main
def thefor =
  val kats =
    for
      kat1 <- Box.observe
      kat2 <- again(kat1)
      kat3 <- again(kat2)
    yield kat3 // what we return here has to be a Contents because it must fit in the Box

  println(kats)
end thefor

@main
def unravelled =
  val kats =
    Box
      .observe
      .flatMap { kat1 =>
        again(kat1).flatMap { kat2 =>
          again(kat2).map { kat3 =>
            kat3
          }
        }
      }

  println(kats)
end unravelled
