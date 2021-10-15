package testcontainers

import org.testcontainers.containers.{
  Network,
  ToxiproxyContainer
}
import zio.Has

object Deps:
  type AppDependencies =
    Has[Network] &
      Has[NetworkAwareness] &
      Has[CareerHistoryServiceT] &
      Has[ToxiproxyContainer]
