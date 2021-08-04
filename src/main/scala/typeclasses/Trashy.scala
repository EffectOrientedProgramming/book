package typeclasses

trait Trash:
  val weight: Int
  val value: Int

trait Bin

case class Paper(weight: Int) extends Trash:
  val value = 5

case object PaperBin extends Bin

case class Cardboard(weight: Int) extends Trash:
  val value = 1

case object CardboardBin extends Bin

def sort(
    sortables: Iterable[Trash]
): Map[Bin, Iterable[Trash]] =
  val mapper =
    (t: Trash) =>
      t match
        case _: Paper =>
          PaperBin
        case _: Cardboard =>
          CardboardBin

  sortables.groupBy(mapper)
end sort

@main
def trashy =
  val paper1     = Paper(1)
  val paper2     = Paper(2)
  val cardboard1 = Cardboard(1)
  println(
    sort(Iterable(paper1, paper2, cardboard1))
  )
