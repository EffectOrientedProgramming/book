package stm

import zio.Console.printLine
import zio.stm.{STM, TRef}
import zio.Runtime.default.unsafe
import zio.Unsafe

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

  Unsafe.unsafe { implicit u =>
    unsafe.run(logic).getOrThrowFiberFailure()
  }
end stmDemo
