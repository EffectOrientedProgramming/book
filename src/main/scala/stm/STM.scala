package stm
import zio.stm.STM
import zio.stm.TRef
import zio.Runtime.default.unsafeRun
import zio.Console.printLine

case class Cash(value: Int)
case class Lumber(value: Int)
case class Grain(value: Int)

case class TownResources(
    cash: Cash,
    lumber: Lumber,
    grain: Grain
)

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
        transferResources(
          treeTown,
          grainVille,
          Cash(3),
          Grain(1)
        ).commit
      finalTreeTownResources <-
        treeTown.get.commit
      _ <- printLine(finalTreeTownResources)
    yield ()

  unsafeRun(logic)
end resourcesDemo

def transferResources[
    A <: Cash | Lumber | Grain,
    B <: Cash | Lumber | Grain
](
    from: TRef[TownResources],
    to: TRef[TownResources],
    fromAmount: A,
    toAmount: B
): STM[Throwable, Unit] =
  for
    senderBalance <- from.get
    _ <-
      fromAmount match
        case c: Cash =>
          if (c.value < senderBalance.cash.value)
            println("Valid cash transaction")
            from.update(fResources =>
              fResources.copy(cash =
                Cash(
                  fResources.cash.value - c.value
                )
              )
            )
//            STM.succeed(())
          else
            STM.fail(
              new Throwable("Not enough cash")
            )
        case l: Lumber =>
//          senderBalance.lumber
          ???
//    _ <-
//      if (amount > senderBalance.cash)
//        STM.fail(
//          new Throwable("insufficient funds")
//        )
//      else
//        from.update(_ - amount) *>
//          to.update(_ + amount)
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
