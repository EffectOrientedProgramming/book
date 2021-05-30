package typeclasses.health

import typeclasses.health.{Canonical, UpStartHealth}

object StateRepresentations {

  object Canonical:
    enum State:
      case Texas, NorthCarolina, Colorado, NewHampshire

  object UpStartHealth:
    enum State:
      case TX, NC, CO, NH

  object InterOp:
    def upstartStateToCanonical(input: UpStartHealth.State): Canonical.State =
      input match {
        case UpStartHealth.State.TX => Canonical.State.Texas
        case UpStartHealth.State.CO => Canonical.State.Colorado
        case UpStartHealth.State.NC => Canonical.State.NorthCarolina
        case UpStartHealth.State.NH => Canonical.State.NewHampshire
      }

}
