package testcontainers

import org.testcontainers.containers.{
  KafkaContainer,
  Network,
  ToxiproxyContainer
}
import testcontainers.QuillLocal.AppPostgresContext
import testcontainers.ServiceDataSets.CareerData
import zio.Has

object Deps:
  type AppDependencies =
    Has[Network] &
      Has[NetworkAwareness] &
      Has[CareerHistoryServiceT] &
      Has[ToxiproxyContainer]

  type RubeDependencies =
    Has[Network] &
      Has[NetworkAwareness] &
      Has[PostgresContainerJ] &
      Has[KafkaContainer] &
      Has[AppPostgresContext] &
      Has[CareerHistoryServiceT] &
      Has[LocationService] &
      Has[BackgroundCheckService] &
      Has[ToxiproxyContainer] &
      Has[CareerData]
end Deps
