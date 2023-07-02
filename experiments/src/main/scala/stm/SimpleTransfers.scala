package stm

import zio.Console.printLine
import zio.stm.{STM, TRef}
import zio.Runtime.default.unsafe
import zio.*
import zio.direct.*

def transfer(
    from: TRef[Int],
    to: TRef[Int],
    amount: Int
): STM[Throwable, Unit] =
  // TODO Figure out if/when zio-direct will
  // handle STM
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

object StmDemo extends ZIOAppDefault:
  def run =
    defer {
      val fromAccount = TRef.make(100).commit.run
      val toAccount   = TRef.make(0).commit.run
      transfer(fromAccount, toAccount, 20)
        .commit
        .run
      //      _ <- transferTransaction.commit
      val toAccountFinal =
        toAccount.get.commit.run
      printLine(
        "toAccountFinal: " + toAccountFinal
      ).run
    }
