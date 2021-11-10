package scenarios.diningphilosophers

/** Problem statement
  *
  * Five silent philosophers sit at a round table
  * with bowls of spaghetti. Forks are placed
  * between each pair of adjacent philosophers.
  *
  * Each philosopher must alternately think and
  * eat. However, a philosopher can only eat
  * spaghetti when they have both left and right
  * forks. Each fork can be held by only one
  * philosopher at a time and so a philosopher
  * can use the fork only if it is not being used
  * by another philosopher. After an individual
  * philosopher finishes eating, they need to put
  * down both forks so that the forks become
  * available to others. A philosopher can only
  * take the fork on their right or the one on
  * their left as they become available and they
  * cannot start eating before getting both
  * forks.
  *
  * Eating is not limited by the remaining
  * amounts of spaghetti or stomach space; an
  * infinite supply and an infinite demand are
  * assumed.
  *
  * The problem is how to design a discipline of
  * behavior (a concurrent algorithm) such that
  * no philosopher will starve; i.e., each can
  * forever continue to alternate between eating
  * and thinking, assuming that no philosopher
  * can know when others may want to eat or
  * think.
  */
class Fork()
case class Philosopher(
    left: Option[Fork] = None,
    right: Option[Fork] = None
):
  def pickupLeftFork(): Philosopher  = ???
  def pickupRightFork(): Philosopher = ???
  def eat(): Philosopher             = ???

/** Table: F1 <-> P1 <-> F2 <-> P2 <-> F3 <-> P3
  * <-> F4 <-> P4 <-> F5 <-> P5 <-> F1
  */

class Table(
    forks: List[Option[Fork]],
    philosophers: List[Philosopher]
):
  val circularForks = forks :+ forks.head

  val alternateRep: Iterator[
    ((Option[Fork], Option[Fork]), Philosopher)
  ] =
    circularForks
      .sliding(2)
      .map(l => (l(0), l(1)))
      .zip(philosophers)

object Dinner:
  val table =
    Table(
      List(
        Some(Fork()),
        Some(Fork()),
        Some(Fork()),
        Some(Fork()),
        Some(Fork())
      ),
      List(
        Philosopher(),
        Philosopher(),
        Philosopher(),
        Philosopher(),
        Philosopher()
      )
    )
