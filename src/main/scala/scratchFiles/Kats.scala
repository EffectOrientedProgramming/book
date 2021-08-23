package scratchFiles

import scala.util.Random

enum Contents:
  case Stop()
  case Continue()

case class Box(contents: Contents):

  def continue(
      f: Contents.Continue => Box
  ): Box =
    contents match
      case stop: Contents.Stop =>
        this
      case continue: Contents.Continue =>
        f(continue)

object Box:

  def observe: Box =
    if Random.nextBoolean() then
      Box(Contents.Stop())
    else
      Box(Contents.Continue())

def again(continue: Contents.Continue): Box =
  Box.observe

@main
def ok =
  val kat1: Box = Box.observe
  println(kat1)
  val kat2: Box = kat1.continue(again)
  println(kat2)
  val kat3: Box =
    kat2.continue(again).continue(again)
  println(kat3)

/* for kat1 <- Box.observe kat2 <- again(kat1)
 * kat3 <- again(kat2) yield kat3 */
