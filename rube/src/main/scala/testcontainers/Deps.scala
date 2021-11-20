package testcontainers

import org.testcontainers.containers.{
  KafkaContainer,
  Network,
  ToxiproxyContainer
}
import testcontainers.QuillLocal.AppPostgresContext
import testcontainers.ServiceDataSets.CareerData

object Deps:
  type AppDependencies =
    Network &
      NetworkAwareness &
      CareerHistoryServiceT &
      ToxiproxyContainer

  type RubeDependencies =
    Network &
      NetworkAwareness &
      PostgresContainerJ &
      KafkaContainer &
      AppPostgresContext &
      CareerHistoryServiceT &
      LocationService &
      BackgroundCheckService &
      ToxiproxyContainer &
      CareerData
end Deps
