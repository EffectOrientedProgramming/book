# STM

This is going to be a tough one.


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/stm/SimpleTransfers.scala
```scala
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

  Unsafe.unsafe { (u: Unsafe) =>
    given Unsafe = u
    unsafe.run(logic).getOrThrowFiberFailure()
  }
end stmDemo

```


### experiments/src/main/scala/stm/TownResources.scala
```scala
package stm

import zio.stm.STM
import zio.stm.TRef
import zio.Runtime.default.unsafe
import zio.Console.printLine
import zio.Unsafe

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

// TODO Consider other names: Commodity
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

  Unsafe.unsafe { (u: Unsafe) =>
    given Unsafe = u
    unsafe.run(logic).getOrThrowFiberFailure()
  }
end resourcesDemo

def tradeResources[
    A <: Resource[A],
    B <: Resource[B]
](
    town1: TRef[TownResources],
    town1Offering: A,
    town2: TRef[TownResources],
    town2Offering: B
): STM[Throwable, Unit] =
  for
    _ <- send(town1, town2, town1Offering)
    _ <- send(town2, town1, town2Offering)
  yield ()

def send[A <: Resource[A], B <: Resource[B]](
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

```

            