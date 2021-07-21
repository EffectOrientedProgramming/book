package typeclasses.trashy3

trait Trash:
  val weight: Int
  val value: Int

trait Bin

case class Paper(weight: Int):
  val value = 5

case object PaperBin extends Bin

case class Cardboard(weight: Int):
  val value = 1

case object CardboardBin extends Bin

case class GoldWatch(karets: Int)

case object NotTrashBin extends Bin

case object ResortBin extends Bin

trait ToBin[T]:
  val bin: Bin
  extension (t: T) def sort: Bin

given ToBin[Paper] with
  val bin = PaperBin
  extension (paper: Paper) def sort = PaperBin

given ToBin[Cardboard] with
  val bin = CardboardBin

  extension (cardboard: Cardboard)
    def sort = CardboardBin

given ToBin[GoldWatch] with
  val bin = NotTrashBin

  extension (watch: GoldWatch)

    def sort =
      if (watch.karets > 1) bin else PaperBin

/*

Extension work great when the typeclass behavior depends on the value of the
T. You don't need an extension method if the mapping (type class) is not based
on a value (i.e. purely the type).

Extension methods aren't just for ergonomics, they are an explicit design choice
that pairs with type classes since most (maybe all) of the time, type classes
need to use the value of the type they are operating on, to do something useful.

 */

given seqToBin[T](using
    t: ToBin[T]
): ToBin[Seq[T]] with
  val bin = t.bin

  extension (s: Seq[T])

    def sort =
      t.bin

/*
given[T <: Product] : ToBin[T] with
  val bin = ResortBin
  extension (p: Product) def sort =
    ???
 */

@main def run =
  val paper1 = Paper(1)
  val paper2 = Paper(2)
  val cardboard1 = Cardboard(1)
  val goldWatch1 = GoldWatch(1)
  val goldWatch2 = GoldWatch(2)
  println(paper1.sort)
  println(cardboard1.sort)
  println(goldWatch1.sort)
  println(goldWatch2.sort)

/*
  println(Seq(paper1, paper2).sort)
  println((paper1, cardboard1).sort)
  println((paper1, paper2, cardboard1, goldWatch1).sort)
 */
//println(sort("asdf")) // does not compile due to lack of type class
//println(sort(Iterable(paper1, paper2, cardboard1)))
