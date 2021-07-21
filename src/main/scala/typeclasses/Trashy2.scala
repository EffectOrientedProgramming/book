package typeclasses.trashy2

trait Trash:
  val weight: Int
  val value: Int

trait Bin

case class Paper(weight: Int) extends Trash:
  val value = 5

case object PaperBin extends Bin

case class Cardboard(weight: Int)
    extends Trash:
  val value = 1

case object CardboardBin extends Bin

case class GoldWatch(karets: Int)

case object NotTrashBin extends Bin

trait TrashToBin[T <: Trash]:
  val bin: Bin

def sortTrash1[T <: Trash](t: T)(using
    toBin: ToBin[T]
): Bin =
  toBin.bin

def sortTrash2[T <: Trash: ToBin](t: T): Bin =
  summon[ToBin[T]].bin

trait ToBin[T]:
  val bin: Bin

def sort1[T](t: T)(using
    toBin: ToBin[T]
): Bin =
  toBin.bin

def sort2[T: ToBin](t: T): Bin =
  summon[ToBin[T]].bin

/*
trait TToBin[T, B <: Bin]

case object PaperToPaperBin extends TToBin[Paper, PaperBin]

def sortT[T, B <: Bin](t: T): Bin =
 */

given ToBin[Paper] with
  val bin = PaperBin

given ToBin[Cardboard] with
  val bin = CardboardBin

given ToBin[GoldWatch] with
  val bin = NotTrashBin

@main def run =
  val paper1 = Paper(1)
  val paper2 = Paper(2)
  val cardboard1 = Cardboard(1)
  val goldWatch = GoldWatch(1)
  println(sort1(paper1))
  println(sort1(paper1))
  println(sort2(cardboard1))
  println(sort2(cardboard1))
  println(sort2(goldWatch))

//println(sort("asdf")) // does not compile due to lack of type class
//println(sort(Iterable(paper1, paper2, cardboard1)))
