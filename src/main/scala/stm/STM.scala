package stm
import zio.stm.STM
import zio.stm.TRef
import zio.Runtime.default.unsafeRun
import zio.Console.printLine

case class Cash(value: Int)
    extends Resource[Cash]

case class Lumber(value: Int)
    extends Resource[Lumber]

case class Grain(value: Int)
    extends Resource[Grain]

sealed trait Resource[A]:
  val value: Int
  def <=(other: Resource[A]): Boolean =
    value <= other.value

case class TownResources(
    cash: Cash,
    lumber: Lumber,
    grain: Grain
):
  def +[A](resource: Resource[A]) =
    resource match
      case c: Cash =>
        copy(cash = Cash(cash.value + c.value))
      case g: Grain =>
        copy(grain =
          Grain(grain.value + g.value)
        )
      case l: Lumber =>
        copy(lumber =
          Lumber(lumber.value + l.value)
        )

  def -[A](resource: Resource[A]) =
    resource match
      case c: Cash =>
        copy(cash = Cash(cash.value - c.value))
      case g: Grain =>
        copy(grain =
          Grain(grain.value - g.value)
        )
      case l: Lumber =>
        copy(lumber =
          Lumber(lumber.value - l.value)
        )

  def canSend[A](resource: Resource[A]) =
    resource match
      case c: Cash =>
        c <= cash
      case l: Lumber =>
        l <= lumber
      case g: Grain =>
        g <= grain
end TownResources

/** Goal: Demonstrate a useful 3 party trade.
  */
@main
def resourcesDemo() =
  val logic =
    for
      treeTown <-
        TRef
          .make(
            TownResources(
              Cash(10),
              Lumber(100),
              Grain(0)
            )
          )
          .commit
      grainVille <-
        TRef
          .make(
            TownResources(
              Cash(0),
              Lumber(0),
              Grain(300)
            )
          )
          .commit
      _ <-
        tradeResources(
          treeTown,
          Cash(3),
          grainVille,
          Grain(30)
        ).commit
      finalTreeTownResources <-
        treeTown.get.commit
      finalGrainVilleResources <-
        grainVille.get.commit
      _ <- printLine(finalTreeTownResources)
      _ <- printLine(finalGrainVilleResources)
    yield ()

  unsafeRun(logic)
end resourcesDemo

def tradeResources[A <: Resource[
  A
], B <: Resource[B]](
    town1: TRef[TownResources],
    town1Offering: A,
    town2: TRef[TownResources],
    town2Offering: B
): STM[Throwable, Unit] =
  for
    _ <-
      sendResources(town1, town2, town1Offering)
    _ <-
      sendResources(town2, town1, town2Offering)
  yield ()

def sendResources[A <: Resource[
  A
], B <: Resource[B]](
    from: TRef[TownResources],
    to: TRef[TownResources],
    resource: A
): STM[Throwable, Unit] =
  for
    senderBalance <- from.get
    canSend = senderBalance.canSend(resource)
    _ <-
      if (canSend)
        from.update(_ - resource) *>
          to.update(_ + resource)
      else
        STM.fail(
          new Throwable(
            "Not enough resources to send: " +
              resource
          )
        )
    extraTransaction =
      from.update(fResources =>
        fResources.copy(cash =
          Cash(fResources.cash.value + 1)
        )
      )
    party2Balance <- to.get
  yield ()

def transfer(
    from: TRef[Int],
    to: TRef[Int],
    amount: Int
): STM[Throwable, Unit] =
  for
    senderBalance <- from.get
    _ <-
      if (amount > senderBalance)
        STM.fail(
          new Throwable("insufficient funds")
        )
      else
        from.update(_ - amount) *>
          to.update(_ + amount)
  yield ()

@main
def stmDemo() =
  val logic =
    for
      fromAccount <- TRef.make(100).commit
      toAccount   <- TRef.make(0).commit
      _ <-
        transfer(fromAccount, toAccount, 20)
          .commit
//      _ <- transferTransaction.commit
      toAccountFinal <- toAccount.get.commit
      _ <-
        printLine(
          "toAccountFinal: " + toAccountFinal
        )
    yield ()

  unsafeRun(logic)
